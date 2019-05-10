package com.zego.common.widgets.log;

import android.app.Application;

import com.zego.common.application.ZegoApplication;


public class EnContext {

    private static final Application INSTANCE = ZegoApplication.zegoApplication;


    public static Application get() {
        return INSTANCE;
    }
}
