package com.zego.common.application;

import android.app.Application;

import com.zego.common.ZGBaseHelper;
import com.zego.common.widgets.log.FloatingView;
import com.zego.common.util.DeviceInfoManager;

/**
 * Created by zego on 2018/10/16.
 */

public class ZegoApplication extends Application {

    public static ZegoApplication zegoApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        zegoApplication = this;

        String userId = DeviceInfoManager.generateDeviceId(this);
        String userName = DeviceInfoManager.getProductName();

        // 添加悬浮日志视图
        FloatingView.get().add();

        // 使用Zego sdk前必须先设置SDKContext。
        ZGBaseHelper.sharedInstance().setSDKContextEx(userId, userName, null, null, 10 * 1024 * 1024, this);

    }
}
