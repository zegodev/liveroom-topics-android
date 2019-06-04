package com.zego.interrupthandler;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.zego.common.util.AppLogger;
import com.zego.zegoliveroom.ZegoLiveRoom;

import java.util.HashMap;
import java.util.Map;

/**
 * 摄像头打断事件处理管理器
 * <p>
 * 1、Android5.0以下，如果其他应用在使用摄像头，回到我们应用后，我们会抢占使用摄像头。（华为荣耀手机只能有一个应用使用摄像头，不允许抢占，所以只能等其他应用释放摄像头，然后回到我们应用才能重新使用摄像头，PS：这个我只测试了一台荣耀4.4的手机，不确定是否所以Android5.0以下的华为荣耀手机都是这样子）
 * 2、Android5.0以上，只要其他应用释放掉摄像头，我们就会重新抢占，不需回到我们应用。
 * 3、Android9.0以上，如果我们应用在后台1分钟，系统将会禁止我们应用使用摄像头，只能重新回到我们应用才能唤醒摄像头。
 */
public class CameraInterruptHandler {

    private final static String TAG = CameraInterruptHandler.class.getSimpleName();

    private CameraInterruptHandler() {
        mCameraManagerActivityLifecycleCallbacks = new CameraManagerActivityLifecycleCallbacks();
    }

    private final static class CameraManagerHolder {
        private final static CameraInterruptHandler sInstance = new CameraInterruptHandler();
    }

    public static CameraInterruptHandler shared() {
        return CameraManagerHolder.sInstance;
    }

    /**
     * 最多管理的Camera数量。
     * <p>
     * 这里只对前后置摄像头进行管理
     */
    private final static int MAX_CAMERA_ID_COUNT = 2;

    private final static int MSG_RESUME_CAMERA = 0x10;

    /**
     * 延时1秒执行唤醒Camera任务
     * <p>
     * 前后置摄像头的切换，将会触发当前摄像头有效，再触发切换的摄像头无效。即假设当前使用的是前置摄像头，切换到后置摄像头的时候，将会先触发前置摄像头的有效回调，再触发后置摄像头的无效回调。<br>
     * 为了避免摄像头切换期间导致满足<b>所有摄像头有效</b>的条件，但其实这个时候摄像头是无效的，所以我们需进行延时唤醒摄像头。<br>
     * 如果是切换摄像头，将会在切换成功，触发摄像头无效回调时，移除延时任务。 {@link #removeResumeCameraTask()}
     */
    private final static int RESUME_CAMERA_TASK_DELAY = 1000;

    /**
     * 需要进入应用才能执行唤醒Camera操作的后台时长。
     * <p>
     * <p>
     * Android9.0以上，如果当前应用进入后台超过1分钟，系统将不允许当前应用使用摄像头，即任何的恢复手段在后台都是无效的，只能应用回到前台后，才能执行唤醒摄像头操作。
     */
    private final static long BACKGROUND_TIME_INTERVAL_NEED_RESUME_CAMERA_FOR_API_29 = 59 * 1000;

    /**
     * UI Handler
     */
    private Handler mUIHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (MSG_RESUME_CAMERA == msg.what) {
                resumeCamera();
            }
        }
    };

    private CameraManagerActivityLifecycleCallbacks mCameraManagerActivityLifecycleCallbacks;

    private ZegoLiveRoom mLiveRoom;

    @TargetApi(21)
    private CameraManager mCameraManager;

    @TargetApi(21)
    private CameraAvailabilityCallback mCameraAvailabilityCallback;

    /**
     * 用于存储前后置摄像头当前的可用状态
     * <p>
     * 主要用于唤醒摄像头，当前后置摄像头都有效的情况下。
     */
    @TargetApi(21)
    private Map<String, Boolean> mCameraIDUsingMap;

    /**
     * 指示是否需要在onStart中唤醒摄像头。
     * <br>
     * Android9.0以上，如果当前应用进入后台超过1分钟，系统将不允许当前应用使用摄像头，即任何的恢复手段在后台都是无效的，只能应用回到前台后，才能执行唤醒摄像头操作。
     */
    @TargetApi(21)
    private boolean mNeedResumeCameraWhileOnStart = false;

    /**
     * 是否允许使用摄像头
     * <p>
     * 当满足唤醒摄像头条件的时候，如果不允许使用摄像头，将不应该唤醒摄像头。
     * <p>
     * 由于外部是不知道是否允许使用摄像头，所以在改变是否允许使用摄像头状态的同时 {@link ZegoLiveRoom#enableCamera(boolean)} ，需在这进行同步，保证唤醒操作的正确性。
     */
    private boolean isCameraEnable = false;

    /**
     * 设置 ApplicationContext 对象，必须执行该方法才能保证功能的正常
     * <br>
     * 内部主要执行了绑定ActivityLifecycleCallbacks 和初始化 {@link CameraManager} 对象
     *
     * @param applicationContext ApplicationContext 对象
     */
    public void setApplicationContext(Application applicationContext) {
        applicationContext.registerActivityLifecycleCallbacks(mCameraManagerActivityLifecycleCallbacks);
        if (isApi21()) {
            initCameraManager(applicationContext);

            // 直到 mCameraManager 和 mLiveRoom 两个变量都被初始化的时候，才执行下面的操作
            if (mLiveRoom != null) {
                initCameraIDUsingMap();
                registerCameraAvailabilityCallback();
            }
        }
    }
    /**
     * 设置 ZegoLiveRoom 对象，必须执行该方法才能保证功能的正常
     * <p>
     * 用于执行唤醒摄像头操作 {@link #resumeCamera()}。
     *
     * @param liveRoom ZegoLiveRoom 对象
     */
    public void setLiveRoom(ZegoLiveRoom liveRoom) {
        this.mLiveRoom = liveRoom;

        // 直到 mCameraManager 和 mLiveRoom 两个变量都被初始化的时候，才执行下面的操作
        if (isApi21() && mCameraManager != null) {
            initCameraIDUsingMap();
            registerCameraAvailabilityCallback();
        }
    }

    /**
     * 是否允许使用摄像头
     * <p>
     * 当满足唤醒摄像头条件的时候，如果不允许使用摄像头，将不应该唤醒摄像头。
     * <p>
     * 由于外部是不知道是否允许使用摄像头，所以在改变是否允许使用摄像头状态的同时 {@link ZegoLiveRoom#enableCamera(boolean)} ，需在这进行同步，保证唤醒操作的正确性。
     *
     * @param enable 是否允许使用 Camera
     */
    public void setCameraEnable(boolean enable) {
        isCameraEnable = enable;
    }

    /**
     * 释放资源，重置变量
     *
     * @param applicationContext ApplicationContext 对象
     */
    public void release(Application applicationContext) {
        applicationContext.unregisterActivityLifecycleCallbacks(mCameraManagerActivityLifecycleCallbacks);

        this.mLiveRoom = null;

        if (isApi21()) {
            // 移除延时唤醒摄像头任务
            mUIHandler.removeCallbacksAndMessages(null);
            mCameraManager.unregisterAvailabilityCallback(mCameraAvailabilityCallback);
            mCameraManager = null;
            mCameraAvailabilityCallback = null;
            mCameraIDUsingMap = null;
            mNeedResumeCameraWhileOnStart = false;
        }
    }

    /**
     * @return 是否Android5.0或以上版本
     */
    private boolean isApi21() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * @return 是否Android9.0或以上版本
     */
    private boolean isApi28() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    @TargetApi(21)
    private void initCameraManager(Application applicationContext) {
        mCameraManager = (CameraManager) applicationContext.getSystemService(Context.CAMERA_SERVICE);
    }

    @TargetApi(21)
    private void registerCameraAvailabilityCallback() {
        mCameraAvailabilityCallback = new CameraAvailabilityCallback();
        mCameraManager.registerAvailabilityCallback(mCameraAvailabilityCallback, null);
    }

    @TargetApi(21)
    private void initCameraIDUsingMap() {
        mCameraIDUsingMap = new HashMap<>();
        // 获取当前所有连接中的摄像头设备ID
        String[] cameraIDList = new String[0];
        try {
            cameraIDList = mCameraManager.getCameraIdList();
        } catch (Exception ignore) {
        }

        // 最多对前后置摄像头进行管理
        int maxCameraIDCount = cameraIDList.length > MAX_CAMERA_ID_COUNT ? MAX_CAMERA_ID_COUNT : cameraIDList.length;
        for (int i = 0; i < maxCameraIDCount; i++) {
            mCameraIDUsingMap.put(cameraIDList[i], true);
        }
    }

    /**
     * 后台时长是否超过指定时长 {@link #BACKGROUND_TIME_INTERVAL_NEED_RESUME_CAMERA_FOR_API_29}
     */
    @TargetApi(28)
    private boolean isMoreThanBackgroundTimeInterval() {
        return ProcessManager.shared().getBackgroundTimeInterval() >= BACKGROUND_TIME_INTERVAL_NEED_RESUME_CAMERA_FOR_API_29;
    }

    /**
     * 启动延时任务去唤醒摄像头
     *
     * @see #resumeCamera()
     * @see #removeResumeCameraTask()
     */
    @TargetApi(21)
    private void startResumeCameraTask() {
        // 如果存在延时唤醒摄像头任务，则返回
        if (mUIHandler.hasMessages(MSG_RESUME_CAMERA)) {
            return;
        }
        // 启动延时任务去唤醒摄像头
        mUIHandler.sendEmptyMessageDelayed(MSG_RESUME_CAMERA, RESUME_CAMERA_TASK_DELAY);
    }

    /**
     * 移除唤醒摄像头的延时任务
     *
     * @see #startResumeCameraTask()
     */
    @TargetApi(21)
    private void removeResumeCameraTask() {
        mUIHandler.removeMessages(MSG_RESUME_CAMERA);
    }

    /**
     * 重新唤醒Camera
     */
    private void resumeCamera() {
        Log.d(TAG, "resumeCamera isCameraEnable: " + isCameraEnable);
        if (isCameraEnable) {
            // enableCamera false 将会导致SDK释放掉旧的不能正常使用的摄像头对象
            mLiveRoom.enableCamera(false);
            // enableCamera true 将会重新创建新的摄像头对象。除了不允许抢占摄像头和Android9.0后台一分钟的场景外，新建的摄像头对象是正常可用的。
            mLiveRoom.enableCamera(true);
        }
    }

    private class CameraManagerActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {
            // Android 5.0以下
            // 由于没有方法监听到摄像头的抢占，为了能保证当前应用持有的摄像头对象可用，当应用回到前台时都需执行唤醒摄像头操作。
            // 当没有出现摄像头被抢占使用的情况下，如果执行唤醒摄像头操作，将会导致拉流端拉到的画面会轻微卡顿一下。
            // 对于华为手机，同时只能一个应用使用摄像头，先用先得，即假设当前应用正在使用摄像头，其他应用将不能使用摄像头，直到前者将摄像头释放。所以可以不需要执行唤醒操作。

            // Android 9.0以上
            // Android9.0以上，如果当前应用进入后台超过1分钟，系统将不允许当前应用使用摄像头，即任何的恢复手段在后台都是无效的，只能应用回到前台后，才能执行唤醒摄像头操作。
            // 所以当应用回到前台时，需根据 mNeedResumeCameraWhileOnStart 的值，确定是否需要唤醒摄像头。
            if (!isApi21() || mNeedResumeCameraWhileOnStart) {
                AppLogger.getInstance().d(CameraInterruptHandler.class, "onActivityStarted mNeedResumeCameraWhileOnStart: " + mNeedResumeCameraWhileOnStart);
                resumeCamera();
                mNeedResumeCameraWhileOnStart = false;
            }
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
     * 摄像头变成可用和不可用回调
     * <p>
     * 系统上所有应用导致的摄像头可用和不可用都会触发该回调。
     */
    @TargetApi(21)
    private class CameraAvailabilityCallback extends CameraManager.AvailabilityCallback {
        /**
         * 当摄像头被释放时会执行该回调。
         * <p>
         * 当前应用暂停使用摄像头，如 {@link ZegoLiveRoom#enableCamera(boolean)} false，也会触发该回调。所以我们只需在允许Camera的时候，才触发唤醒的逻辑。
         * <p>
         * 由于部分机型允许同时使用前后置摄像头，部分机型只允许同时刻只能使用前置或者后置摄像头其中一个。<br>
         * 为了方便管理，这里统一认为，当前后置摄像头都有效的情况下，才允许唤醒摄像头。
         * <p>
         * 前后置摄像头的切换，将会触发当前摄像头有效，再触发切换的摄像头无效。即假设当前使用的是前置摄像头，切换到后置摄像头的时候，将会先触发前置摄像头的有效回调，再触发后置摄像头的无效回调。<br>
         * 为了避免摄像头切换期间导致满足<b>所有摄像头有效</b>的条件，但其实这个时候是无效的，所以我们需进行延时唤醒摄像头，延时时间为 {@link #RESUME_CAMERA_TASK_DELAY}。<br>
         * 如果是切换摄像头，将会在切换成功，触发摄像头无效回调时，移除延时任务。 {@link #removeResumeCameraTask()}
         * <p>
         * Android9.0以上，如果当前应用进入后台超过1分钟，系统将不允许当前应用使用摄像头，即任何的恢复手段在后台都是无效的，只能应用回到前台后，才能执行唤醒摄像头操作。
         *
         * @param cameraId 摄像头ID，0是前置摄像头，1是后置摄像。{@link android.hardware.Camera.CameraInfo}
         */
        @Override
        public void onCameraAvailable(String cameraId) {
            AppLogger.getInstance().d(CameraInterruptHandler.class, "onCameraAvailable cameraID: " + cameraId);
            // 设置 对于CameraID 的 Camera是否可用
            if (mCameraIDUsingMap.containsKey(cameraId)) {
                mCameraIDUsingMap.put(cameraId, true);

                // 如果允许使用摄像头，才需检查是否需要执行唤醒摄像头操作。
                if (isCameraEnable) {
                    boolean isAllCameraAvailable = true;

                    // 检查当前是否所有摄像头都是可用状态
                    for (String key : mCameraIDUsingMap.keySet()) {
                        Boolean isCameraAvailable = mCameraIDUsingMap.get(key);
                        if (isCameraAvailable == null || !isCameraAvailable) {
                            isAllCameraAvailable = false;
                            break;
                        }
                    }
                    if (isAllCameraAvailable) {
                        // Android9.0以上，如果当前应用进入后台超过1分钟，系统将不允许当前应用使用摄像头，即任何的恢复手段在后台都是无效的，只能应用回到前台后，才能执行唤醒摄像头操作。
                        if (isApi28() && isMoreThanBackgroundTimeInterval()) {
                            AppLogger.getInstance().d(CameraInterruptHandler.class, "onCameraAvailable isMoreThanBackgroundTimeInterval");
                            mNeedResumeCameraWhileOnStart = true;
                        } else {
                            AppLogger.getInstance().d(CameraInterruptHandler.class, "onCameraAvailable startResumeCameraTask");
                            mNeedResumeCameraWhileOnStart = false;
                            startResumeCameraTask();
                        }
                    }
                }
            }
        }

        /**
         * 当摄像头被使用时会执行该回调。
         * <p>
         * 摄像头抢占形式根据机型和优先级区分成这两种，允许抢占和不允许抢占。<br>
         * 允许抢占即指只要有别的应用需要使用摄像头，当前应用持有的摄像头对象将不能正常工作，即摄像头被抢占了。<br>
         * 不允许抢占即指只要当前应用正在使用摄像头，之后其他应用获取到的摄像头对象将不能正常工作。<br>
         * 基于上面两种情况（其实我们只需考虑第一种情况，即允许抢占的情况），如果当前应用出现了被抢占的情况，我们是不需要做任何处理，因为当前应用持有的摄像头对象不能正常工作，体现效果相当于 {@link ZegoLiveRoom#enableCamera(boolean)} false。
         * <p>
         * 我们在这个方法中需要做的事情是，记录无效的摄像头，等到所有摄像头都有效的时候，我们尝试唤醒摄像头。
         * <p>
         * <p>
         * <b>请不要在该方法中执行 {@link ZegoLiveRoom#enableCamera(boolean)} false，因为当前应用使用摄像头也会导致该方法的执行。</b>
         *
         * @param cameraId 摄像头ID，0是前置摄像头，1是后置摄像。{@link android.hardware.Camera.CameraInfo}
         */
        @Override
        public void onCameraUnavailable(String cameraId) {
            AppLogger.getInstance().d(CameraInterruptHandler.class, "onCameraUnavailable cameraID: " + cameraId);
            // 设置 对于CameraID 的 Camera是否可用
            if (mCameraIDUsingMap.containsKey(cameraId)) {
                mCameraIDUsingMap.put(cameraId, false);
                mNeedResumeCameraWhileOnStart = false;
                removeResumeCameraTask();
            }
        }
    }
}
