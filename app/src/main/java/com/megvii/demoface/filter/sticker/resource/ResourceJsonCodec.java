package com.megvii.demoface.filter.sticker.resource;

import com.megvii.demoface.filter.sticker.bean.DynamicSticker;
import com.megvii.demoface.filter.sticker.bean.DynamicStickerData;
import com.megvii.demoface.filter.sticker.bean.DynamicStickerFrameData;
import com.megvii.demoface.filter.sticker.bean.DynamicStickerNormalData;
import com.megvii.demoface.filter.sticker.bean.StaticStickerNormalData;
import com.megvii.demoface.filter.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * json解码器
 */
public class ResourceJsonCodec {

    /**
     * 读取默认动态贴纸数据
     * @param folderPath      json文件所在文件夹路径
     * @return
     * @throws IOException
     * @throws JSONException
     */
    public static DynamicSticker decodeStickerData(String folderPath)
            throws IOException, JSONException {

        File file = new File(folderPath, "json");
        String stickerJson = FileUtils.convertToString(new FileInputStream(file));

        JSONObject jsonObject = new JSONObject(stickerJson);
        DynamicSticker dynamicSticker = new DynamicSticker();
        dynamicSticker.unzipPath = folderPath;
        if (dynamicSticker.dataList == null) {
            dynamicSticker.dataList = new ArrayList<>();
        }

        JSONArray stickerList = jsonObject.getJSONArray("stickerList");
        for (int i = 0; i < stickerList.length(); i++) {
            JSONObject jsonData = stickerList.getJSONObject(i);
            String type = jsonData.getString("type");
            DynamicStickerData data;
            if ("sticker".equals(type)) {
                data = new DynamicStickerNormalData();
                JSONArray centerIndexList = jsonData.getJSONArray("centerIndexList");
                ((DynamicStickerNormalData) data).centerIndexList = new int[centerIndexList.length()];
                for (int j = 0; j < centerIndexList.length(); j++) {
                    ((DynamicStickerNormalData) data).centerIndexList[j] = centerIndexList.getInt(j);
                }
                ((DynamicStickerNormalData) data).offsetX = (float) jsonData.getDouble("offsetX");
                ((DynamicStickerNormalData) data).offsetY = (float) jsonData.getDouble("offsetY");
                ((DynamicStickerNormalData) data).baseScale = (float) jsonData.getDouble("baseScale");
                ((DynamicStickerNormalData) data).startIndex = jsonData.getInt("startIndex");
                ((DynamicStickerNormalData) data).endIndex = jsonData.getInt("endIndex");
            }else if ("static".equals(type)) {//静态贴纸
                data = new StaticStickerNormalData();
                ((StaticStickerNormalData) data).alignMode = jsonData.getInt("alignMode");
            } else {
                // 如果不是贴纸又不是前景的话，则直接跳过
                if (!"frame".equals(type)) {
                    continue;
                }
                data = new DynamicStickerFrameData();
                ((DynamicStickerFrameData) data).alignMode = jsonData.getInt("alignMode");
            }
            DynamicStickerData stickerData = data;
            stickerData.width = jsonData.getInt("width");
            stickerData.height = jsonData.getInt("height");
            stickerData.frames = jsonData.getInt("frames");
            stickerData.action = jsonData.getInt("action");
            stickerData.stickerName = jsonData.getString("stickerName");
            stickerData.duration = jsonData.getInt("duration");
            stickerData.stickerLooping = (jsonData.getInt("stickerLooping") == 1);
            stickerData.audioPath = jsonData.optString("audioPath");
            stickerData.audioLooping = (jsonData.optInt("audioLooping", 0) == 1);
            stickerData.maxCount = jsonData.optInt("maxCount", 5);

            dynamicSticker.dataList.add(stickerData);
        }

        return dynamicSticker;
    }

}
