package com.zego.interrupthandler;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.zego.common.util.AppLogger;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.constants.ZegoConstants;

/**
 * 音频打断事件处理管理器
 * <p>
 * 该功能建立于电话、QQ微信语音和视频通话、QQ微信短语音短视频等都是短暂失去焦点。
 * <p>
 * 1、当电话、QQ微信语音和视频通话、QQ微信短语音短视频等场景，都会触发短暂失去焦点，此时将会暂停使用音频设备，等到焦点回归后，将恢复使用音频设备
 * 2、当网易云音乐、游戏、播放器等情况，将会触发不确定时长的失去焦点，此时将会暂停使用音频设备；等到焦点回归或者回到应用尝试获取焦点成功后，将恢复使用音频设备
 * 3、Android9.0以上，如果我们应用在后台1分钟，系统将会禁止我们应用使用麦克风，只能重新回到我们应用才能重新使用麦克风，不需要做任何操作，前提是麦克风没有被别的应用占用。
 */
public class AudioInterruptHandler {

    private final static String TAG = AudioInterruptHandler.class.getSimpleName();

    private AudioInterruptHandler() {
        mOnAudioFocusChangeListener = new OnAudioFocusChangeListener();
        mActivityLifecycleCallbacks = new AudioFocusManagerActivityLifecycleCallbacks();
    }

    private final static class AudioFocusManagerHolder {
        private final static AudioInterruptHandler sInstance = new AudioInterruptHandler();
    }

    public static AudioInterruptHandler shared() {
        return AudioFocusManagerHolder.sInstance;
    }

    private final static int MSG_RESUME_AUDIO = 0x10;

    /**
     * 延时1秒执行恢复音频设备任务
     * <p>
     * 为了避免音频焦点恢复时，麦克风其实还没有完整释放导致麦克风不能正常恢复问题，需延时1秒执行恢复音频设备任务<br>
     * 同时，避免短暂时间内频繁丢失重获音频焦点的情况下，导致的无用操作。（QQ小视频录制-开始录制-录制完成效果，将会导致 丢失音频焦点-获取音频焦点-丢失音频焦点）
     */
    private final static int RESUME_AUDIO_TASK_DELAY = 1000;

    private Handler mUIHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (MSG_RESUME_AUDIO == msg.what) {
                resumeAudioModuleIfNeed();
            }
        }
    };

    private ZegoLiveRoom mLiveRoom;

    private AudioManager mAudioManager;

    private OnAudioFocusChangeListener mOnAudioFocusChangeListener;

    private AudioFocusManagerActivityLifecycleCallbacks mActivityLifecycleCallbacks;

    /**
     * 是否持有音频焦点。
     * <p>
     * 短暂失去音频焦点不会影响该值。
     */
    private boolean isAudioFocusGranted = false;

    /**
     * 是否允许使用音频设备
     * <p>
     * 当满足恢复音频设备条件的时候，如果不允许使用音频设备，将不应该恢复音频设备。
     * <p>
     * 由于外部是不知道是否允许使用音频设备，所以在改变是否允许使用摄像头状态的同时 {@link ZegoLiveRoom#pauseModule(int)}、 {@link ZegoLiveRoom#resumeModule(int)} ，需在这进行同步，保证恢复音频设备操作的正确性。
     */
    private boolean isAudioModuleEnable = false;

    /**
     * 设置 ApplicationContext 对象，必须执行该方法才能保证功能的正常
     * <br>
     * 内部主要执行了绑定ActivityLifecycleCallbacks 和初始化 {@link AudioManager} 对象
     *
     * @param applicationContext ApplicationContext 对象
     */
    public void setApplicationContext(Application applicationContext) {
        applicationContext.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
        mAudioManager = (AudioManager) applicationContext.getSystemService(Context.AUDIO_SERVICE);

        // 直到 mAudioManager 和 mLiveRoom 两个变量都被初始化的时候，才会尝试请求获取音频焦点
        if (mLiveRoom != null) {
            requestAudioFocusIfNeed();
        }
    }

    /**
     * 设置 ZegoLiveRoom 对象，必须执行该方法才能保证功能的正常
     * <p>
     * 用于暂停音频设备和恢复音频设备。
     *
     * @param liveRoom ZegoLiveRoom 对象
     */
    public void setLiveRoom(ZegoLiveRoom liveRoom) {
        mLiveRoom = liveRoom;

        // 直到 mAudioManager 和 mLiveRoom 两个变量都被初始化的时候，才会尝试请求获取音频焦点
        if (mAudioManager != null) {
            requestAudioFocusIfNeed();
        }
    }

    /**
     * 设置是否允许使用音频设备
     * <p>
     * 当满足恢复音频设备条件的时候，如果不允许使用音频设备，将不应该恢复音频设备。
     * <p>
     * 由于外部是不知道是否允许使用音频设备，所以在改变是否允许使用摄像头状态的同时 {@link ZegoLiveRoom#pauseModule(int)}、 {@link ZegoLiveRoom#resumeModule(int)} ，需在这进行同步，保证恢复音频设备操作的正确性。
     *
     * @param enable 是否允许使用音频设备
     */
    public void setAudioModuleEnable(boolean enable) {
        isAudioModuleEnable = enable;
    }

    /**
     * 释放资源，重置变量
     *
     * @param applicationContext ApplicationContext 对象
     */
    public void release(Application applicationContext) {
        applicationContext.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);

        mUIHandler.removeCallbacksAndMessages(null);

        this.mLiveRoom = null;

        mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        mAudioManager = null;
        isAudioFocusGranted = false;
    }

    /**
     * 获取到焦点
     */
    private void onAudioFocusGain() {
        startResumeAudioTaskIfNeed();
    }

    /**
     * 短暂失去焦点后重新获取回焦点
     */
    private void onAudioFocusRegain() {
        startResumeAudioTaskIfNeed();
    }

    /**
     * 短暂失去焦点
     */
    private void onAudioFocusLossTransient() {
        removeResumeAudioTask();
        mLiveRoom.pauseModule(ZegoConstants.ModuleType.AUDIO);
    }

    /**
     * 不确定时长的失去焦点
     */
    private void onAudioFocusLoss() {
        removeResumeAudioTask();
        mLiveRoom.pauseModule(ZegoConstants.ModuleType.AUDIO);
    }

    /**
     * 恢复使用音频设备
     */
    private void resumeAudioModuleIfNeed() {
        AppLogger.getInstance().d(AudioInterruptHandler.class, "resumeAudioModuleIfNeed isAudioModuleEnable: " + isAudioModuleEnable);
        // 只有在允许使用音频设备的情况下，才进行恢复操作。
        if (isAudioModuleEnable) {
            mLiveRoom.resumeModule(ZegoConstants.ModuleType.AUDIO);
        }
    }

    /**
     * 启动延时任务恢复使用音频设备
     *
     * @see #resumeAudioModuleIfNeed()
     * @see #removeResumeAudioTask()
     */
    private void startResumeAudioTaskIfNeed() {
        AppLogger.getInstance().d(AudioInterruptHandler.class, "startResumeAudioTaskIfNeed isAudioModuleEnable: " + isAudioModuleEnable);
        if (isAudioModuleEnable) {
            mUIHandler.sendEmptyMessageDelayed(MSG_RESUME_AUDIO, RESUME_AUDIO_TASK_DELAY);
        }
    }

    /**
     * 移除恢复音频设备的延时任务
     *
     * @see #startResumeAudioTaskIfNeed
     */
    private void removeResumeAudioTask() {
        mUIHandler.removeMessages(MSG_RESUME_AUDIO);
    }

    /**
     * 尝试请求音频焦点。
     */
    private void requestAudioFocusIfNeed() {
        AppLogger.getInstance().d(AudioInterruptHandler.class, "requestAudioFocusIfNeed");
        // 如果已经持有了音频焦点，这不需要请求。
        if (!isAudioFocusGranted) {
            int ref = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);
            AppLogger.getInstance().d(AudioInterruptHandler.class, "requestAudioFocus ref: " + ref);
            if (ref == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // 获取到音频焦点
                isAudioFocusGranted = true;
                onAudioFocusGain();
            } else {
                // 不能获取音频焦点
                isAudioFocusGranted = false;
                onAudioFocusLoss();
            }
        }
    }

    private final class AudioFocusManagerActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {
            // 如果是不确定时长的失去焦点，并且麦克风是可用的情况下，我们将会尝试获取焦点
            requestAudioFocusIfNeed();
        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }

    /**
     * 音频焦点改变监听器
     */
    private final class OnAudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
            AppLogger.getInstance().d(AudioInterruptHandler.class, "onAudioFocusChange focusChange: " + focusChange);
            switch (focusChange) {
                // 获取到焦点
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (isAudioFocusGranted) {
                        // 如果之前已经获取过焦点，证明这一次是由于短暂失去焦点后，重新获取到焦点。
                        onAudioFocusRegain();
                    } else {
                        // 之前是不确定时长失去焦点。
                        isAudioFocusGranted = true;
                        onAudioFocusGain();
                    }
                    break;
                // 不确定时长失去焦点
                case AudioManager.AUDIOFOCUS_LOSS:
                    isAudioFocusGranted = false;
                    onAudioFocusLoss();
                    break;
                // 短暂时长获取焦点
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    onAudioFocusLossTransient();
                    break;
            }
        }
    }
}
