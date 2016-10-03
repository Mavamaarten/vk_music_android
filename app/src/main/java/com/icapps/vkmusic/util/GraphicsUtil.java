package com.icapps.vkmusic.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import rx.Observable;

/**
 * Created by maartenvangiel on 03/10/16.
 */
public class GraphicsUtil {

    public static Observable<Boolean> isBottomDark(Bitmap bitmap) {
        return Observable.fromCallable(() -> calculateIsDark(bitmap));
    }

    private static boolean calculateIsDark(Bitmap bitmap) {
        boolean dark = false;

        float darkThreshold = bitmap.getWidth() * (bitmap.getHeight() / 5) * 0.55f;
        int darkPixels = 0;

        int[] pixels = new int[bitmap.getWidth() * (bitmap.getHeight() / 5)];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, bitmap.getHeight() / 5 * 4, bitmap.getWidth(), bitmap.getHeight() / 5);

        for (int pixel : pixels) {
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);
            double luminance = (0.299 * r + 0.0f + 0.587 * g + 0.0f + 0.114 * b + 0.0f);
            if (luminance < 150) {
                darkPixels++;
            }
        }

        if (darkPixels >= darkThreshold) {
            dark = true;
        }

        return dark;
    }

}
