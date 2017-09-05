/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.schaffer.zxinglibrary.decode;

import android.content.Intent;
import android.net.Uri;

import com.google.zxing.BarcodeFormat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class DecodeFormatManager {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    static final Set<BarcodeFormat> PRODUCT_FORMATS;
    static final Set<BarcodeFormat> INDUSTRIAL_FORMATS;
    private static final Set<BarcodeFormat> ONE_D_FORMATS;
    static final Set<BarcodeFormat> QR_CODE_FORMATS = EnumSet.of(BarcodeFormat.QR_CODE);
    static final Set<BarcodeFormat> DATA_MATRIX_FORMATS = EnumSet.of(BarcodeFormat.DATA_MATRIX);
    static final Set<BarcodeFormat> AZTEC_FORMATS = EnumSet.of(BarcodeFormat.AZTEC);
    static final Set<BarcodeFormat> PDF417_FORMATS = EnumSet.of(BarcodeFormat.PDF_417);

    static {
        PRODUCT_FORMATS = EnumSet.of(BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.RSS_14,
                BarcodeFormat.RSS_EXPANDED);
        INDUSTRIAL_FORMATS = EnumSet.of(BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.ITF,
                BarcodeFormat.CODABAR);
        ONE_D_FORMATS = EnumSet.copyOf(PRODUCT_FORMATS);
        ONE_D_FORMATS.addAll(INDUSTRIAL_FORMATS);
    }

    private static final Map<String, Set<BarcodeFormat>> FORMATS_FOR_MODE;

    static {
        FORMATS_FOR_MODE = new HashMap<>();
        FORMATS_FOR_MODE.put(Scan.ONE_D_MODE, ONE_D_FORMATS);
        FORMATS_FOR_MODE.put(Scan.PRODUCT_MODE, PRODUCT_FORMATS);
        FORMATS_FOR_MODE.put(Scan.QR_CODE_MODE, QR_CODE_FORMATS);
        FORMATS_FOR_MODE.put(Scan.DATA_MATRIX_MODE, DATA_MATRIX_FORMATS);
        FORMATS_FOR_MODE.put(Scan.AZTEC_MODE, AZTEC_FORMATS);
        FORMATS_FOR_MODE.put(Scan.PDF417_MODE, PDF417_FORMATS);
    }

    private DecodeFormatManager() {
    }

    static Set<BarcodeFormat> parseDecodeFormats(Intent intent) {
        Iterable<String> scanFormats = null;
        CharSequence scanFormatsString = intent.getStringExtra(Scan.FORMATS);
        if (scanFormatsString != null) {
            scanFormats = Arrays.asList(COMMA_PATTERN.split(scanFormatsString));
        }
        return parseDecodeFormats(scanFormats, intent.getStringExtra(Scan.MODE));
    }

    static Set<BarcodeFormat> parseDecodeFormats(Uri inputUri) {
        List<String> formats = inputUri.getQueryParameters(Scan.FORMATS);
        if (formats != null && formats.size() == 1 && formats.get(0) != null) {
            formats = Arrays.asList(COMMA_PATTERN.split(formats.get(0)));
        }
        return parseDecodeFormats(formats, inputUri.getQueryParameter(Scan.MODE));
    }

    private static Set<BarcodeFormat> parseDecodeFormats(Iterable<String> scanFormats, String decodeMode) {
        if (scanFormats != null) {
            Set<BarcodeFormat> formats = EnumSet.noneOf(BarcodeFormat.class);
            try {
                for (String format : scanFormats) {
                    formats.add(BarcodeFormat.valueOf(format));
                }
                return formats;
            } catch (IllegalArgumentException iae) {
                // ignore it then
            }
        }
        if (decodeMode != null) {
            return FORMATS_FOR_MODE.get(decodeMode);
        }
        return null;
    }

    public static final class Scan {
        /**
         * Send this intent to open the Barcodes app in scanning mode, find a barcode, and return
         * the results.
         */
        public static final String ACTION = "com.schaffer.zxinglibrary.SCAN";

        /**
         * By default, sending this will decode all barcodes that we understand. However it
         * may be useful to limit scanning to certain formats. Use
         * {@link android.content.Intent#putExtra(String, String)} with one of the values below.
         * <p>
         * Setting this is effectively shorthand for setting explicit formats with {@link #FORMATS}.
         * It is overridden by that setting.
         */
        public static final String MODE = "SCAN_MODE";

        /**
         * Decode only UPC and EAN barcodes. This is the right choice for shopping apps which get
         * prices, reviews, etc. for products.
         */
        public static final String PRODUCT_MODE = "PRODUCT_MODE";

        /**
         * Decode only 1D barcodes.
         */
        public static final String ONE_D_MODE = "ONE_D_MODE";

        /**
         * Decode only QR codes.
         */
        public static final String QR_CODE_MODE = "QR_CODE_MODE";

        /**
         * Decode only Data Matrix codes.
         */
        public static final String DATA_MATRIX_MODE = "DATA_MATRIX_MODE";

        /**
         * Decode only Aztec.
         */
        public static final String AZTEC_MODE = "AZTEC_MODE";

        /**
         * Decode only PDF417.
         */
        public static final String PDF417_MODE = "PDF417_MODE";

        /**
         * Comma-separated list of formats to scan for. The values must match the names of
         * {@link com.google.zxing.BarcodeFormat}s, e.g. {@link com.google.zxing.BarcodeFormat#EAN_13}.
         * Example: "EAN_13,EAN_8,QR_CODE". This overrides {@link #MODE}.
         */
        public static final String FORMATS = "SCAN_FORMATS";

        /**
         * Optional parameter to specify the id of the camera from which to recognize barcodes.
         * Overrides the default camera that would otherwise would have been selected.
         * If provided, should be an int.
         */
        public static final String CAMERA_ID = "SCAN_CAMERA_ID";

        /**
         * @see com.google.zxing.DecodeHintType#CHARACTER_SET
         */
        public static final String CHARACTER_SET = "CHARACTER_SET";

        /**
         * Optional parameters to specify the width and height of the scanning rectangle in pixels.
         * The app will try to honor these, but will clamp them to the size of the preview frame.
         * You should specify both or neither, and pass the size as an int.
         */
        public static final String WIDTH = "SCAN_WIDTH";
        public static final String HEIGHT = "SCAN_HEIGHT";

        /**
         * Desired duration in milliseconds for which to pause after a successful scan before
         * returning to the calling intent. Specified as a long, not an integer!
         * For example: 1000L, not 1000.
         */
        public static final String RESULT_DISPLAY_DURATION_MS = "RESULT_DISPLAY_DURATION_MS";

        /**
         * Prompt to show on-screen when scanning by intent. Specified as a {@link String}.
         */
        public static final String PROMPT_MESSAGE = "PROMPT_MESSAGE";

        /**
         * If a barcode is found, Barcodes returns {@link android.app.Activity#RESULT_OK} to
         * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
         * of the app which requested the scan via
         * {@link android.app.Activity#startActivityForResult(android.content.Intent, int)}
         * The barcodes contents can be retrieved with
         * {@link android.content.Intent#getStringExtra(String)}.
         * If the user presses Back, the result code will be {@link android.app.Activity#RESULT_CANCELED}.
         */
        public static final String RESULT = "SCAN_RESULT";

        /**
         * Call {@link android.content.Intent#getStringExtra(String)} with {@link #RESULT_FORMAT}
         * to determine which barcode format was found.
         * See {@link com.google.zxing.BarcodeFormat} for possible values.
         */
        public static final String RESULT_FORMAT = "SCAN_RESULT_FORMAT";

        /**
         * Call {@link android.content.Intent#getStringExtra(String)} with {@link #RESULT_UPC_EAN_EXTENSION}
         * to return the content of any UPC extension barcode that was also found. Only applicable
         * to {@link com.google.zxing.BarcodeFormat#UPC_A} and {@link com.google.zxing.BarcodeFormat#EAN_13}
         * formats.
         */
        public static final String RESULT_UPC_EAN_EXTENSION = "SCAN_RESULT_UPC_EAN_EXTENSION";

        /**
         * Call {@link android.content.Intent#getByteArrayExtra(String)} with {@link #RESULT_BYTES}
         * to get a {@code byte[]} of raw bytes in the barcode, if available.
         */
        public static final String RESULT_BYTES = "SCAN_RESULT_BYTES";

        /**
         * Key for the value of {@link com.google.zxing.ResultMetadataType#ORIENTATION}, if available.
         * Call {@link android.content.Intent#getIntArrayExtra(String)} with {@link #RESULT_ORIENTATION}.
         */
        public static final String RESULT_ORIENTATION = "SCAN_RESULT_ORIENTATION";

        /**
         * Key for the value of {@link com.google.zxing.ResultMetadataType#ERROR_CORRECTION_LEVEL}, if available.
         * Call {@link android.content.Intent#getStringExtra(String)} with {@link #RESULT_ERROR_CORRECTION_LEVEL}.
         */
        public static final String RESULT_ERROR_CORRECTION_LEVEL = "SCAN_RESULT_ERROR_CORRECTION_LEVEL";

        /**
         * Prefix for keys that map to the values of {@link com.google.zxing.ResultMetadataType#BYTE_SEGMENTS},
         * if available. The actual values will be set under a series of keys formed by adding 0, 1, 2, ...
         * to this prefix. So the first byte segment is under key "SCAN_RESULT_BYTE_SEGMENTS_0" for example.
         * Call {@link android.content.Intent#getByteArrayExtra(String)} with these keys.
         */
        public static final String RESULT_BYTE_SEGMENTS_PREFIX = "SCAN_RESULT_BYTE_SEGMENTS_";

        /**
         * Setting this to false will not save scanned codes in the history. Specified as a {@code boolean}.
         */
        public static final String SAVE_HISTORY = "SAVE_HISTORY";

        private Scan() {
        }
    }
}
