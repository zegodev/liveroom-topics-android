package com.zego.sound.processing;


import com.zego.common.ZGBaseHelper;
import com.zego.common.util.AppLogger;
import com.zego.zegoavkit2.audioprocessing.ZegoAudioProcessing;
import com.zego.zegoavkit2.audioprocessing.ZegoAudioReverbMode;
import com.zego.zegoavkit2.audioprocessing.ZegoAudioReverbParam;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.constants.ZegoAvConfig;

/**
 * ZGSoundProcessingHelper
 * <p>
 * 此类将SDK 音效配置参数项 进行封装, 简化接口。
 * 开发者可参考该类的代码, 注释, 理解 SDK 接口
 *
 * 有关变声/混响/立体声的详细说明请查看ZEGO官方文档: <a>https://doc.zego.im/CN/665.html</a>
 */
public class ZGSoundProcessingHelper {

    public static ZGSoundProcessingHelper zgConfigHelper;

    // 音频混响参数配置
    public ZegoAudioReverbParam zegoAudioReverbParam = new ZegoAudioReverbParam();

    //*************** 声音的变化***************************
    // 无
    public static final float VOICE_CHANGE_NO = 0.0f;

    // 萝莉
    public static final float VOICE_CHANGE_LOLI = 7.0f;

    // 大叔
    public static final float VOICE_CHANGE_UNCLE = -3f;


    public static ZGSoundProcessingHelper sharedInstance() {
        if (zgConfigHelper == null) {
            synchronized (ZGSoundProcessingHelper.class) {
                if (zgConfigHelper == null) {
                    zgConfigHelper = new ZGSoundProcessingHelper();
                }
            }
        }
        return zgConfigHelper;
    }

    /**
     * 是否initSDK
     *
     * @return true 代表initSDK完成, false 代表initSDK失败
     */
    private boolean isInitSDKSuccess() {
        if (ZGBaseHelper.sharedInstance().getZGBaseState() != ZGBaseHelper.ZGBaseState.InitSuccessState) {
            AppLogger.getInstance().w(ZGSoundProcessingHelper.class, "设置失败! SDK未初始化, 请先初始化SDK");
            return false;
        }
        return true;
    }


    /**
     * 启用采集监听 注意!!! 只有插入耳机时才生效
     * 推流时可调用本 API 进行参数配置。开启采集监听，自己讲话可以听到自己的声音
     * <p>
     * 调用时机: 初始化之后
     *
     * @param enable true为启用, false为关闭
     */
    public void enableLoopback(boolean enable) {
        AppLogger.getInstance().i(ZGSoundProcessingHelper.class, enable ? "开启耳返" : "关闭耳返");
        if (isInitSDKSuccess()) {
            ZegoLiveRoom zegoLiveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.enableLoopback(enable);
        }
    }

    /**
     * 设置变声器参数。
     * 变声音效只针对采集的声音有效， 参数取值范围[-8.0, 8.0]，
     * 几种典型的变声效果参考 ZegoAudioProcessing.ZegoVoiceChangerCategory
     * <p>
     * 调用时机: 初始化SDK之后
     *
     * @param param
     */
    public void setVoiceChangerParam(float param) {
        AppLogger.getInstance().i(ZGSoundProcessingHelper.class, "设置变声参数 %f", param);
        if (isInitSDKSuccess()) {
            ZegoAudioProcessing.setVoiceChangerParam(param);
        }
    }

    /**
     * 启用混响
     * <p>
     * 调用时机: 初始化SDK之后
     *
     * @param enable true 为启用, false 为关闭
     * @param mode   可以参考{@link ZegoAudioReverbMode}
     */
    public void enableReverb(boolean enable, ZegoAudioReverbMode mode) {
        AppLogger.getInstance().i(ZGSoundProcessingHelper.class, enable ? "启用混响 %s" : "关闭混响 %s", mode.name());
        if (isInitSDKSuccess()) {
            ZegoAudioProcessing.enableReverb(enable, mode);
        }
    }


    /**
     * 设置房间大小
     * 房间大小，取值范围[0.0, 1.0]，用于控制产生混响"房间"的大小，房间越大，混响越强
     * <p>
     * 调用时机: 初始化SDK之后
     *
     * @param roomSize
     */
    public void setReverbRoomSize(float roomSize) {
        if (isInitSDKSuccess()) {
            AppLogger.getInstance().i(ZGSoundProcessingHelper.class, "设置混响房间大小: %f", roomSize);
            zegoAudioReverbParam.roomSize = roomSize;
            ZegoAudioProcessing.setReverbParam(zegoAudioReverbParam);
        }
    }

    /**
     * 设置混响阻尼
     * 混响阻尼， 取值范围[0.0， 2.0]，控制混响的衰减程度，阻尼越大，衰减越大
     * <p>
     * 调用时机: 初始化SDK之后
     *
     * @param reverbDamping
     */
    public void setReverbDamping(float reverbDamping) {
        if (isInitSDKSuccess()) {
            AppLogger.getInstance().i(ZGSoundProcessingHelper.class, "设置混响阻尼: %f", reverbDamping);
            zegoAudioReverbParam.damping = reverbDamping;
            ZegoAudioProcessing.setReverbParam(zegoAudioReverbParam);
        }
    }

    /**
     * 设置余响
     * 余响，取值范围[0.0, 0.5]，用于控制混响的拖尾长度
     * <p>
     * 调用时机: 初始化SDK之后
     *
     * @param reverberance
     */
    public void setReverberance(float reverberance) {
        if (isInitSDKSuccess()) {
            AppLogger.getInstance().i(ZGSoundProcessingHelper.class, "设置余响: %f", reverberance);
            zegoAudioReverbParam.reverberance = reverberance;
            ZegoAudioProcessing.setReverbParam(zegoAudioReverbParam);
        }
    }

    /**
     * 设置干湿比
     * 干湿比，取值范围 大于等于 0.0。 控制混响与直达声和早期反射声之间的比例，
     * 干(dry)的部分默认定为1，当干湿比设为较小时，湿(wet)的比例较大，此时混响较强
     * <p>
     * 调用时机: 初始化SDK之后
     *
     * @param dryWetRation
     */
    public void setDryWetRation(float dryWetRation) {
        if (isInitSDKSuccess()) {
            AppLogger.getInstance().i(ZGSoundProcessingHelper.class, "设置干湿比: %f", dryWetRation);
            zegoAudioReverbParam.dryWetRatio = dryWetRation;
            ZegoAudioProcessing.setReverbParam(zegoAudioReverbParam);
        }
    }

    /**
     * 启用虚拟立体声
     * 通过深度使用双声道技术，虚拟出发音源的各个位置角度，实现立体声，3D环绕音，听声辩位等效果
     * 注意!!! 虚拟立体声需要在推流前打开双声道才可以使用，
     * 可在{@link ZegoLiveRoom#setAudioChannelCount}中设置双声道
     * <p>
     * 调用时机: 初始化SDK之后
     *
     * @param param
     */
    public void enableVirtualStereo(boolean enable, int param) {
        if (isInitSDKSuccess()) {
            AppLogger.getInstance().i(ZGSoundProcessingHelper.class, enable ? "启用虚拟立体声: %d" : "关闭虚拟立体声: %d", param);
            ZegoAudioProcessing.enableVirtualStereo(enable, param);
        }
    }

    /**
     * 设置推流音频声道数.
     * 注意：
     * 1. 必须在推流前设置.
     * 2. setLatencyMode设置为 ZegoConstants.LatencyMode#Normal ,
     * ZegoConstants.LatencyMode#Normal2,
     * ZegoConstants.LatencyMode#Low3 才能设置双声道
     * 3. 在移动端双声道通常需要配合音频前处理才能体现效果.
     * <p>
     * 调用时机: 初始化SDK之后, 推流之前
     *
     * @param audioChannelCount
     */
    public void setAudioChannelCount(int audioChannelCount) {
        if (isInitSDKSuccess()) {
            AppLogger.getInstance().i(ZGSoundProcessingHelper.class, "设置推流声道数: %d", audioChannelCount);
            ZGBaseHelper.sharedInstance().getZegoLiveRoom().setAudioChannelCount(audioChannelCount);
        }
    }
}
