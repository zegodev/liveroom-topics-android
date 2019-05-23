package com.zego.interrupthandler;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.zego.common.util.AppLogger;

/**
 * 前后台应用管理
 * <h2>主要功能：</h2>
 * <ol>
 *     <li>判断当前应用是否在前台。</li>
 *     <li>获取当前应用后台形式存在的时长。（回到前台将清零）</li>
 * </ol>
 */
public class ProcessManager {

    /**
     * TODO 如果在Application初始化的时候执行 {@link ProcessManager#setApplicationContext}，下面的值需修改为1。
     * TODO 如果在Activity的onCreate方法中执行 {@link ProcessManager#setApplicationContext}，下面的值需修改为0。
     */
    private final static int ACTIVITY_COUNT_OF_FOREGROUND = 0;

    private ProcessManager() {
        mProcessManagerActivityLifecycleCallbacks = new ProcessManagerActivityLifecycleCallbacks();
    }

    private final static class ProcessManagerHolder {
        private final static ProcessManager sInstance = new ProcessManager();
    }

    public static ProcessManager shared() {
        return ProcessManagerHolder.sInstance;
    }

    private ProcessManagerActivityLifecycleCallbacks mProcessManagerActivityLifecycleCallbacks;

    private int mForegroundActivityCount = 0;

    /**
     * 进入后台的时间，具体时间点。
     * <br>
     * 当在前台，为-1
     */
    private long mTimeToBeBackground = -1;

    /**
     * 获取当前进场是否有Activity在前台
     *
     * @return 返回当前进场是否有Activity在前台
     */
    public boolean isForeground() {
        return mForegroundActivityCount >= ACTIVITY_COUNT_OF_FOREGROUND;
    }


    /**
     * 获取进入后台的时长
     *
     * @return 进入后台的时长，当应用在前台的时候调用这个方法，则返回0；
     */
    public long getBackgroundTimeInterval() {
        if (mTimeToBeBackground == -1) {
            return 0;
        }
        return System.currentTimeMillis() - mTimeToBeBackground;
    }

    /**
     * 必须执行该方法绑定Application，相关功能才会生效。
     * @param applicationContext ApplicationContext 对象
     */
    public void setApplicationContext(Application applicationContext) {
        applicationContext.registerActivityLifecycleCallbacks(mProcessManagerActivityLifecycleCallbacks);
    }

    /**
     * 释放相关资源
     * @param applicationContext ApplicationContext 对象
     */
    public void release(Application applicationContext) {
        applicationContext.unregisterActivityLifecycleCallbacks(mProcessManagerActivityLifecycleCallbacks);
        mTimeToBeBackground = -1;
        mForegroundActivityCount = 0;
    }

    private class ProcessManagerActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {
            mForegroundActivityCount++;
            AppLogger.getInstance().d(ProcessManager.class, "onActivityStarted mForegroundActivityCount: " + mForegroundActivityCount);
            if (isForeground()) {
                mTimeToBeBackground = -1;
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
            mForegroundActivityCount--;
            AppLogger.getInstance().d(ProcessManager.class, "onActivityStopped mForegroundActivityCount: " + mForegroundActivityCount);
            if (!isForeground()) {
                mTimeToBeBackground = System.currentTimeMillis();
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }
}
