package com.icapps.vkmusic.util;

import android.content.Context;

/**
 * Created by maartenvangiel on 13/09/16.
 */
public class SizeUtil {

    public static int dpToPixels(int dp, Context context){
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }

    public static int pixelsToDp(int pixels, Context context){
        float density = context.getResources().getDisplayMetrics().density;
        return (int)(pixels / density);
    }

}
