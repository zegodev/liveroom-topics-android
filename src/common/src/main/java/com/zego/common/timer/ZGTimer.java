package com.zego.common.timer;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

/**
 * Created by zego on 2018/10/18.
 */

public abstract class ZGTimer {

    /**
     * Millis since epoch when alarm should stop.
     */
    /**
     * The interval in millis that the user receives callbacks
     */
    private final long mCountdownInterval;

    /**
     * boolean representing if the timer was cancelled
     */
    private boolean mCancelled = false;


    public ZGTimer(long countDownInterval) {
        mCountdownInterval = countDownInterval;
    }

    /**
     * Cancel the countdown.
     */
    public synchronized final void cancel() {
        mCancelled = true;
        mHandler.removeMessages(MSG);
    }

    /**
     * Start the countdown.
     */
    public synchronized final ZGTimer start() {
        mCancelled = false;
        mHandler.sendMessage(mHandler.obtainMessage(MSG));
        return this;
    }

    /**
     * Callback fired when the time is up.
     */
    public abstract void onFinish();


    private static final int MSG = 1;


    // handles counting down
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            synchronized (ZGTimer.this) {
                if (mCancelled) {
                    return;
                }
                onFinish();
                sendMessageDelayed(obtainMessage(MSG), mCountdownInterval);

            }
        }
    };
}