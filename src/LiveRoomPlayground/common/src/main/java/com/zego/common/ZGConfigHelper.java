package com.zego.common;


import com.zego.common.util.AppLogger;
import com.zego.zegoavkit2.audioprocessing.ZegoAudioProcessing;
import com.zego.zegoavkit2.audioprocessing.ZegoAudioReverbMode;
import com.zego.zegoavkit2.audioprocessing.ZegoAudioReverbParam;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.constants.ZegoAvConfig;

/**
 * ZGConfigHelper
 * <p>
 * 此类将SDK 配置参数项 进行封装, 简化接口。
 * 开发者可参考该类的代码, 理解 SDK 接口
 */
public class ZGConfigHelper {

    public static ZGConfigHelper zgConfigHelper;
    // ZEGO 音视频参数配置
    public ZegoAvConfig zegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.High);

    // 音频混响参数配置
    public ZegoAudioReverbParam zegoAudioReverbParam = new ZegoAudioReverbParam();

    public Boolean getZgCameraState() {
        return zgCameraState;
    }

    private void setZgCameraState(Boolean zgCameraState) {
        this.zgCameraState = zgCameraState;
    }

    public Boolean getZgMicState() {
        return zgMicState;
    }

    private void setZgMicState(Boolean zgMicState) {
        this.zgMicState = zgMicState;
    }

    private static Boolean zgCameraState = true;
    private static Boolean zgMicState = true;


    public static ZGConfigHelper sharedInstance() {
        if (zgConfigHelper == null) {
            synchronized (ZGConfigHelper.class) {
                if (zgConfigHelper == null) {
                    zgConfigHelper = new ZGConfigHelper();
                }
            }
        }
        return zgConfigHelper;
    }

    /**
     * 启用摄像头
     * <p>
     * 调用时机: 初始化之后
     *
     * @param enable true 为开启, false 为关闭
     */
    public void enableCamera(boolean enable) {
        if (isInitSDKSuccess()) {
            AppLogger.getInstance().i(ZGConfigHelper.class, enable ? "启用摄像头" : "关闭摄像头");
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.enableCamera(enable);
            this.setZgCameraState(enable);
        }
    }

    /**
     * 启用麦克风
     * <p>
     * 调用时机: 初始化之后
     *
     * @param enable true 为启用麦克风
     */
    public void enableMic(boolean enable) {
        AppLogger.getInstance().i(ZGConfigHelper.class, enable ? "启用麦克风" : "关闭麦克风");
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.enableMic(enable);
            this.setZgMicState(enable);
        }
    }

    /**
     * 是否initSDK
     *
     * @return true 代表initSDK完成, false 代表initSDK失败
     */
    private boolean isInitSDKSuccess() {
        if (ZGBaseHelper.sharedInstance().getZGBaseState() != ZGBaseHelper.ZGBaseState.InitSuccessState) {
            AppLogger.getInstance().w(ZGConfigHelper.class, "设置失败! SDK未初始化, 请先初始化SDK");
            return false;
        }
        return true;
    }

    /**
     * 开启扬声器
     * <p>
     * 调用时机: 初始化之后
     *
     * @param enable
     */
    public void enableSpeaker(boolean enable) {
        AppLogger.getInstance().i(ZGConfigHelper.class, enable ? "开启扬声器" : "关闭扬声器");
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.enableSpeaker(enable);
        }
    }

    /**
     * 开启拉流镜像
     * <p>
     * 调用时机: 初始化之后
     *
     * @param enable
     * @param streamId
     */
    public void enablePlayMirror(boolean enable, String streamId) {
        AppLogger.getInstance().i(ZGConfigHelper.class, enable ? "开启拉流镜像" : "关闭拉流镜像");
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.enableViewMirror(enable, streamId);
        }
    }

    /**
     * 设置推流预览视图模式
     * sdk内置3种视图模式
     * 1: 等比缩放填充整View，可能有部分被裁减。 SDK 默认值。{@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleAspectFill}
     * 2: 等比缩放，可能有黑边 {@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleAspectFit}
     * 3: 填充整个View，视频可能会变形。{@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleToFill}
     * <p>
     * <p>
     * 调用时机: 无要求
     *
     * @param viewMode 视图模式 {@link com.zego.zegoliveroom.constants.ZegoVideoViewMode}
     */
    public void setPreviewViewMode(int viewMode) {
        AppLogger.getInstance().i(ZGConfigHelper.class, "设置预览视图模式 viewMode : %d", viewMode);
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.setPreviewViewMode(viewMode);
        }
    }

    /**
     * 启用硬解
     * 开启硬解后，sdk会使用GPU去处理视频解码，会大大减少app对CPU的占用率,
     * 达到省电不发热的效果。
     * <p>
     * 调用时机: 无要求
     *
     * @param enable true为启用, false为关闭
     */
    public void enableHardwareDecode(boolean enable) {
        AppLogger.getInstance().i(ZGConfigHelper.class, enable ? "开启硬解" : "关闭硬解");

        ZegoLiveRoom.requireHardwareDecoder(enable);
    }

    /**
     * 启用硬编
     * 开启硬编后，sdk会使用GPU去处理视频编码，会大大减少app对CPU的占用率,
     * 达到省电不发热的效果。
     * <p>
     * <p>
     * 调用时机: 无要求
     *
     * @param enable true为启用, false为关闭
     */
    public void enableHardwareEncode(boolean enable) {
        AppLogger.getInstance().i(ZGConfigHelper.class, enable ? "开启硬编" : "关闭硬编");

        ZegoLiveRoom.requireHardwareEncoder(enable);
    }


    /**
     * 前置摄像头开关
     * <p>
     * sdk默认采集设备用的前置摄像头, 如果需要切换成后置摄像头
     * 可以关闭前置摄像头开关。
     * <p>
     * 调用时机: 初始化SDK之后
     *
     * @param frontCam false为后置摄像头, true为前置摄像头
     */
    public void setFrontCam(boolean frontCam) {
        AppLogger.getInstance().i(ZGConfigHelper.class, frontCam ? "开启前置摄像头" : "关闭前置摄像头");
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.setFrontCam(frontCam);
        }
    }

    /**
     * 设置拉流音量
     * <p>
     * sdk默认采集设备用的前置摄像头, 如果需要切换成后置摄像头
     * 可以关闭前置摄像头开关。
     * <p>
     * 调用时机: 拉流之后
     *
     * @param playVolume 音量大小 0 ~ 100
     */
    public void setPlayVolume(int playVolume) {
        AppLogger.getInstance().i(ZGConfigHelper.class, "设置拉流音量, volume : %d", playVolume);
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.setPlayVolume(playVolume);
        }
    }

    /**
     * 设置拉流预览视图模式
     * sdk内置3种视图模式
     * 1: 等比缩放填充整View，可能有部分被裁减。 SDK 默认值。{@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleAspectFill}
     * 2: 等比缩放，可能有黑边 {@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleAspectFit}
     * 3: 填充整个View，视频可能会变形。{@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleToFill}
     * <p>
     * 调用时机: 拉流之后
     *
     * @param viewMode 视图模式 {@link com.zego.zegoliveroom.constants.ZegoVideoViewMode}
     * @param streamID 开发者需要改变哪条streamID视图就传对应的streamID
     */
    public void setPlayViewMode(int viewMode, String streamID) {
        AppLogger.getInstance().i(ZGConfigHelper.class, "设置拉流视图模式 viewMode : %d, streamID : %s", viewMode, streamID);
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.setViewMode(viewMode, streamID);
        }
    }

    /**
     * 设置推流分辨率
     * <p>
     * 调用时机: 初始化SDK之后
     *
     * @param width
     * @param height
     */
    public void setPublishResolution(int width, int height) {
        AppLogger.getInstance().i(ZGConfigHelper.class, "设置推流分辨率 width : %d, height : %d", width, height);
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            // 设置sdk编码分辨率
            zegoAvConfig.setVideoCaptureResolution(width, height);
            // 设置摄像头采集的分辨率
            zegoAvConfig.setVideoEncodeResolution(width, height);
            zegoLiveRoom.setAVConfig(zegoAvConfig);
        }
    }

    /**
     * 设置视频码率
     * 码率越高，每秒传送数据就越多，画质就越清晰. 同时更耗带宽
     * <p>
     * 调用时机: 初始化SDK之后
     *
     * @param videoBitrate 码率值
     */
    public void setVideoBitrate(int videoBitrate) {
        AppLogger.getInstance().i(ZGConfigHelper.class, "设置视频码率 videoBitrate : %d", videoBitrate);
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoAvConfig.setVideoBitrate(videoBitrate);
            zegoLiveRoom.setAVConfig(zegoAvConfig);
        }
    }

    /**
     * 设置视频推流帧率
     * <p>
     * 调用时机: 初始化SDK之后
     *
     * @param fps 帧率就是在1秒钟时间里传输的图像的数量。帧率越高画面越流畅. 同时更耗带宽
     */
    public void setPublishVideoFps(int fps) {
        AppLogger.getInstance().i(ZGConfigHelper.class, "设置视频帧率 fps : %d", fps);
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoAvConfig.setVideoFPS(fps);
            zegoLiveRoom.setAVConfig(zegoAvConfig);
        }
    }

    /**
     * 启用预览镜像
     * 开启后自己看到的画面为镜像
     * <p>
     * 调用时机: 初始化之后
     *
     * @param enable true为启用, false为关闭
     */
    public void enablePreviewMirror(boolean enable) {
        AppLogger.getInstance().i(ZGConfigHelper.class, enable ? "开启预览镜像" : "关闭预览镜像");
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.enablePreviewMirror(enable);
        }
    }

}
