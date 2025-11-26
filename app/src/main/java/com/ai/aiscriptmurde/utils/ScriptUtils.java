package com.ai.aiscriptmurde.utils;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ScriptUtils {
    public static String readAssetFile(Context context, String fileName) {
        try{
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getResId(Context context, String imgName) {
        if (imgName == null || imgName.isEmpty()) {
            return 0;
        }

        return context.getResources().getIdentifier(
                imgName,
                "drawable",
                context.getPackageName()
        );
    }
}
