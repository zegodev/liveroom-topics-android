package com.zego.mediaplayer;

import android.content.Context;
import android.util.Log;

import com.zego.mediaplayer.entity.ZGResourcesInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zego on 2018/10/16.
 */

public class ZGMediaPlayerDemoHelper {

    static ZGMediaPlayerDemoHelper zgMediaPlayerDemoHelper;

    public static ZGMediaPlayerDemoHelper sharedInstance() {
        synchronized (ZGMediaPlayerDemoHelper.class) {
            if (zgMediaPlayerDemoHelper == null) {
                zgMediaPlayerDemoHelper = new ZGMediaPlayerDemoHelper();
            }
        }
        return zgMediaPlayerDemoHelper;
    }

    private List<ZGResourcesInfo> mediaList = new ArrayList<>();


    public List<ZGResourcesInfo> getMediaList() {
        return mediaList;
    }

    public ZGMediaPlayerDemoHelper() {
        mediaList.add(new ZGResourcesInfo()
                .mediaNameKey("audio clip(-50% tempo)")
                .mediaFileTypeKey("mp3")
                .mediaSourceTypeKey("local")
                .mediaUrlKey("sample_-50_tempo")
        );

        mediaList.add(new ZGResourcesInfo()
                .mediaNameKey("ad")
                .mediaFileTypeKey("mp4")
                .mediaSourceTypeKey("local")
                .mediaUrlKey("ad")
        );

        mediaList.add(new ZGResourcesInfo()
                .mediaNameKey("audio clip")
                .mediaFileTypeKey("mp3")
                .mediaSourceTypeKey("online")
                .mediaUrlKey("https://storage.zego.im/demo/sample_orig.mp3")
        );

        mediaList.add(new ZGResourcesInfo()
                .mediaNameKey("大海")
                .mediaFileTypeKey("mp4")
                .mediaSourceTypeKey("online")
                .mediaUrlKey("https://storage.zego.im/demo/201808270915.mp4")
        );
    }




    private static boolean isFileByName(String string) {
        if (string.contains(".")) {
            return true;
        }
        return false;
    }

    public String getPath(Context context, String fileName) {
        String path = context.getExternalCacheDir().getPath();
        File pathFile = new File(path + "/" + fileName);
        if (!pathFile.exists()) {
            copyFileFromAssets(context, fileName, pathFile.getPath());
        }
        return pathFile.getPath();
    }

    /**
     * 从assets目录下拷贝文件到存储卡.
     *
     * @param context            安卓上下文：用于获取 assets 目录下的资源
     * @param assetsFilePath     assets文件的路径名如：xxx.mp3
     * @param targetFileFullPath sd卡目标文件路径如：/sdcard/xxx.mp3
     *
     */
    public static void copyFileFromAssets(Context context, String assetsFilePath, String targetFileFullPath) {
        Log.d("Tag", "copyFileFromAssets ");
        InputStream assetsFileInputStream;
        try {
            assetsFileInputStream = context.getAssets().open(assetsFilePath);
            copyFile(assetsFileInputStream, targetFileFullPath);
        } catch (IOException e) {
            Log.d("Tag", "copyFileFromAssets " + "IOException-" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void copyFile(InputStream in, String targetPath) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(targetPath));
            byte[] buffer = new byte[1024];
            int byteCount = 0;
            while ((byteCount = in.read(buffer)) != -1) {// 循环从输入流读取
                // buffer字节
                fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
            }
            fos.flush();// 刷新缓冲区
            in.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
