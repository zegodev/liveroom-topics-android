package com.zego.common.ui;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.zego.common.widgets.log.FloatingView;

/**
 * Created by zego on 2019/2/19.
 */

public class BaseActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    protected void onStart() {
        super.onStart();

        // 在应用内实现悬浮窗，需要依附Activity生命周期
        FloatingView.get().attach(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // // 在应用内实现悬浮窗，需要依附Activity生命周期
        FloatingView.get().detach(this);
    }

    // 需要申请 麦克风权限-读写sd卡权限-摄像头权限
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO"};


    /**
     * 校验并请求权限
     */
    public boolean checkOrRequestPermission(int code) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS_STORAGE, code);
                return false;
            }
        }
        return true;
    }

}
