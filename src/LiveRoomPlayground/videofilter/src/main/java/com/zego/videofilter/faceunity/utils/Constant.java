package com.zego.videofilter.faceunity.utils;

import android.os.Environment;

import java.io.File;

/**
 * Created by tujh on 2018/2/7.
 */

public class Constant {

    public static final String APP_NAME = "VideoFilterDemo";
    public static final String filePath = Environment.getExternalStoragePublicDirectory("")
            + File.separator + "FaceUnity" + File.separator + APP_NAME + File.separator;
}
