package com.zego.audioplayer;

import android.content.Context;
import android.util.Log;

import com.zego.zegoavkit2.audioplayer.ZegoAudioPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ZGAudioPlayerHelper {

    private static ZGAudioPlayerHelper zgAudioPlayerHelper = null;

    private ZegoAudioPlayer zegoAudioPlayer = null;

    public static ZGAudioPlayerHelper sharedInstance() {
        synchronized (ZGAudioPlayerHelper.class) {
            if (zgAudioPlayerHelper == null) {
                zgAudioPlayerHelper = new ZGAudioPlayerHelper();
            }
        }

        return zgAudioPlayerHelper;
    }

    // 获取音效播放器实例
    public ZegoAudioPlayer getZegoAudioPlayer() {
        if (zegoAudioPlayer == null) {
            zegoAudioPlayer = new ZegoAudioPlayer();
        }
        return zegoAudioPlayer;
    }

    public void destoryAudioPlayer() {
        if (zegoAudioPlayer != null) {
            zegoAudioPlayer.destroyAudioPlayer();
        }
        zegoAudioPlayer = null;
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
