package com.schaffer.zxinglibrary.decode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.ui.ImageGridActivity;
import com.schaffer.zxinglibrary.R;
import com.schaffer.zxinglibrary.camera.CameraManager;
import com.schaffer.zxinglibrary.view.ViewfinderView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class CaptureActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = CaptureActivity.class.getSimpleName();


    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Collection<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_with_photos);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        ambientLightManager = new AmbientLightManager(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    @Override
    protected void onResume() {
        super.onResume();


        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinderView);
        viewfinderView.setCameraManager(cameraManager);
        handler = null;

        beepManager.updatePrefs();
        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();


        decodeFormats = null;
        characterSet = null;

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        //historyManager = null; // Keep for onActivityResult
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new com.schaffer.zxinglibrary.decode.CaptureActivityHandler(this, decodeFormats, characterSet, cameraManager);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
        }
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        Log.d("wxl", "rawResult=" + rawResult);

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            String resultString = rawResult.getText();
            // Then not from history, so beep/vibrate and we have an image to draw on
            beepManager.playBeepSoundAndVibrate();
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("result", resultString);
            resultIntent.putExtras(bundle);
            this.setResult(RESULT_OK, resultIntent);
        } else {
            Toast.makeText(CaptureActivity.this, "Scan failed!", Toast.LENGTH_SHORT).show();
        }
        CaptureActivity.this.finish();

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }


    public static final int REQUEST_CODE_FOR_PHOTO = 11;

    /**
     * take photo from imagepicker and decode the Qrcode from it.
     *
     * @param view
     */
    public void takePhotoForDecode(View view) {
        Intent intent = new Intent(this, ImageGridActivity.class);
        startActivityForResult(intent, REQUEST_CODE_FOR_PHOTO);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {
            if (data != null && requestCode == REQUEST_CODE_FOR_PHOTO) {
                ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                if (images.size() > 0) {
//                    Intent toAdvertint = new Intent(this, AdvertActivity.class);
//                    toAdvertint.putExtra("imagePath", images.get(0));
//                    startActivityForResult(toAdvertint, GET_SURE_ADVERT_IMG);
                    Bitmap bitmap = adjustBitmap(images.get(0).path);
//                    Bitmap bitmap = BitmapFactory.decodeFile(images.get(0).path);
                    int byteCount = bitmap.getByteCount();
                    Log.w("schaffer", "Bitmap-->" + byteCount / 1024 / 1024 + "M");
                    decodeQRCode(bitmap);
                }
            }
        }
    }


    public final Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);

    public void decodeQRCode(final Bitmap bitmap) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int[] pixels = new int[width * height];
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                    RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                    Result result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), HINTS);
                    return result.getText();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                Log.w("schaffer", "result=" + result);
                if (result == null) {
                    Toast.makeText(CaptureActivity.this, "扫描失败", Toast.LENGTH_SHORT).show();
                } else {
                    Intent resultIntent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("result", result);
                    resultIntent.putExtras(bundle);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            }
        }.execute();

    }


    public Bitmap adjustBitmap(String pathName) {

        BitmapFactory.Options opt = new BitmapFactory.Options();
        // 这个isjustdecodebounds很重要
        opt.inJustDecodeBounds = true;
        Bitmap bm = BitmapFactory.decodeFile(pathName, opt);

        // 获取到这个图片的原始宽度和高度
        int picWidth = opt.outWidth;
        int picHeight = opt.outHeight;

        // 获取屏的宽度和高度
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();

        // isSampleSize是表示对图片的缩放程度，比如值为2图片的宽度和高度都变为以前的1/2
        opt.inSampleSize = 1;
        // 根据屏的大小和图片大小计算出缩放比例
        if (picWidth > picHeight) {
            if (picWidth > screenWidth)
                opt.inSampleSize = picWidth / screenWidth;
        } else {
            if (picHeight > screenHeight)
                opt.inSampleSize = picHeight / screenHeight;
        }
        // 这次再真正地生成一个有像素的，经过缩放了的bitmap
        opt.inJustDecodeBounds = false;
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(pathName, opt);
    }

}
