package com.zego.playground.demo;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.zego.zegoliveroom.ZegoLiveRoom;

/**
 * Created by zego on 2018/10/16.
 */

public class ZegoApplication extends Application {

    static ZegoApplication zegoApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        zegoApplication = this;

        ZegoLiveRoom.setSDKContext(new ZegoLiveRoom.SDKContext() {
            @Nullable
            @Override
            public String getSoFullPath() {
                return null;
            }

            @Nullable
            @Override
            public String getLogPath() {
                return null;
            }

            @NonNull
            @Override
            public Application getAppContext() {
                return zegoApplication;
            }
        });
    }
}
