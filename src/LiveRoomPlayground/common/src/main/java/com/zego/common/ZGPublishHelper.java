package com.zego.common;

import android.support.annotation.NonNull;
import android.view.View;

import com.zego.common.util.AppLogger;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherExCallback;

/**
 * ZGPublishHelper
 *
 * 推流帮助类
 * 主要简化SDK推流一系列接口
 * 开发者可参考该类的代码, 理解SDK接口
 *
 * 注意!!! 开发者需要先初始化sdk, 登录房间后, 才能进行推流
 *
 */

public class ZGPublishHelper {

    public static ZGPublishHelper zgPublishHelper;

    public static ZGPublishHelper sharedInstance() {
        if (zgPublishHelper == null) {
            synchronized (ZGPublishHelper.class) {
                if (zgPublishHelper == null) {
                    zgPublishHelper = new ZGPublishHelper();
                }
            }
        }
        return zgPublishHelper;
    }

    /**
     * 开始预览
     * 可以调用{@link ZegoLiveRoom#setPreviewView(Object)} 更新渲染视图
     *
     * @param view 要渲染的视图，sdk会把采集到的数据渲染到view上,
     */
    public void startPreview(@NonNull View view) {
        if (!isInitSDKSuccess()) {
            AppLogger.getInstance().w(ZGPublishHelper.class, "推流预览失败, 请先初始化sdk");
            return;
        }
        AppLogger.getInstance().i(ZGPublishHelper.class, "开始预览");
        ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
        zegoLiveRoom.setPreviewView(view);
        zegoLiveRoom.startPreview();
    }

    /**
     * 开始推流
     * 注意!!! 登陆房间后才能使用推流接口，该接口要与 {@link #stopPublishing()} 成对使用。
     * <li><a href="https://doc.zego.im/CN/490.html"> 推流常见问题 </a>
     *
     * @param streamID streamID，不能为空，只支持长度不超过 256 byte 的数字，下划线，字母。
     *                 注意!!! 每个用户的流名必须保持唯一，也就是流名必须appID全局唯一，
     *                 也不能包含特殊字符。
     * @param title    标题，长度不可超过 255 byte
     * @param flag     推流标记, 详见 {@link com.zego.zegoliveroom.constants.ZegoConstants.PublishFlag}
     * @return true 为推流成功 false 为推流失败
     */
    public boolean startPublishing(@NonNull String streamID, @NonNull String title, int flag) {
        if (!isInitSDKSuccess()) {
            AppLogger.getInstance().w(ZGPublishHelper.class, "推流失败, 请先初始化sdk再进行推流");
            return false;
        }
        AppLogger.getInstance().i(ZGPublishHelper.class, "开始推流, streamID : %s, title : %s, flag : %s", streamID, title, flag);
        ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
        return zegoLiveRoom.startPublishing(streamID, title, flag);
    }


    /**
     * 停止推流
     */
    public void stopPublishing() {
        if (!isInitSDKSuccess()) {
            AppLogger.getInstance().w(ZGPublishHelper.class, "停止推流失败, 请先初始化sdk");
            return;
        }
        AppLogger.getInstance().i(ZGPublishHelper.class, "停止推流");
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().stopPublishing();
    }

    /**
     * 停止预览
     *
     * 注意!!! 停止预览后并不会停止推流，需要停止推流请调用 {@link #stopPublishing()}
     */
    public void stopPreviewView() {
        if (!isInitSDKSuccess()) {
            AppLogger.getInstance().w(ZGPublishHelper.class, "停止预览失败, 请先初始化sdk");
            return;
        }
        AppLogger.getInstance().i(ZGPublishHelper.class, "停止预览");
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().stopPreview();
    }


    /**
     * 是否initSDK
     *
     * @return true 代表initSDK完成, false 代表initSDK失败
     */
    private boolean isInitSDKSuccess() {
        if (ZGBaseHelper.sharedInstance().getZGBaseState() != ZGBaseHelper.ZGBaseState.InitSuccessState) {

            return false;
        }
        return true;
    }

    /**
     *
     * 推流代理很重要, 开发者可以按自己的需求在回调里实现自己的业务
     * app相关业务。回调介绍请参考文档<a>https://doc.zego.im/CN/209.html</>
     *
     * @param publisherCallback 推流代理
     */
    public void setPublisherCallback(IZegoLivePublisherCallback publisherCallback) {
        if (isInitSDKSuccess()) {
            ZGBaseHelper.sharedInstance().getZegoLiveRoom().setZegoLivePublisherCallback(publisherCallback);
        }else{
            AppLogger.getInstance().w(ZGBaseHelper.class, "设置推流代理失败! SDK未初始化, 请先初始化SDK");
        }
    }

    /**
     * 释放推流的代理
     * 当不再使用ZegoSDK时，可以释放推流的代理
     * <p>
     * 调用时机：建议在unInitSDK之前设置。
     *
     */
    public void releasePublisherCallback() {
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().setZegoLivePublisherCallback(null);
    }
}
