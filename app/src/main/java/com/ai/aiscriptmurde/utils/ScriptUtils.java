package com.ai.aiscriptmurde.utils;

import android.content.Context;
import android.content.res.AssetManager;
import com.ai.aiscriptmurde.model.ScriptModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ScriptUtils {

    /**
     * 从 assets 文件夹读取文本文件内容
     */
    public static String readAssetFile(Context context, String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * 🔥 新增：获取所有剧本列表的摘要信息
     * @param context 上下文
     * @return 剧本模型列表
     */
    public static List<ScriptModel> getScriptList(Context context) {
        String jsonStr = readAssetFile(context, "mock_data/script_list.json");
        if (jsonStr != null) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<ScriptModel>>() {}.getType();
            return gson.fromJson(jsonStr, listType);
        }
        return null;
    }

    /**
     * 根据资源名称获取资源ID
     */
    public static int getResId(Context context, String resName) {
        return context.getResources().getIdentifier(resName, "drawable", context.getPackageName());
    }
}