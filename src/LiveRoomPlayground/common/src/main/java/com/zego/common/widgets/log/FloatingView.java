package com.zego.common.widgets.log;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.zego.common.R;
import com.zego.common.util.DeviceInfoManager;


public class FloatingView implements IFloatingView, Handler.Callback {
    private boolean isShowState = false; // false 为隐藏 true 为显示
    private EnFloatingView mEnFloatingView;
    private LogView mLogView;
    private static volatile FloatingView mInstance;
    // 从activity里获取到的根容器，用来添加悬浮组件的
    private FrameLayout mContainer;
    private Handler handler = null;
    private volatile boolean isStopTimer = true;

    private FloatingView() {
        HandlerThread handlerThread = new HandlerThread("timer");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper(), this);
    }

    public static FloatingView get() {
        if (mInstance == null) {
            synchronized (FloatingView.class) {
                if (mInstance == null) {
                    mInstance = new FloatingView();
                }
            }
        }
        return mInstance;
    }

    /**
     * 通过此函数可以在悬浮视图里面添加日志。
     *
     * @param log 日志内容。
     */
    public void addLog(final String log) {
        if (mLogView != null) {

            // 避免在多个不同线程操作，切换到ui线程队列
            mLogView.post(new Runnable() {
                @Override
                public void run() {

                    mLogView.logAdapter.addLog(log);
                    mLogView.scrollToBottom(); // 为了用户查看日志，每次添加log时都滚动到底部最新日志。

                }
            });
        }
    }

    /**
     * 清空日志
     */
    public void clearLog() {
        if (mLogView != null) {
            // 避免在多个不同线程操作，切换到ui线程队列
            mLogView.post(new Runnable() {
                @Override
                public void run() {
                    mLogView.logAdapter.clear();
                }
            });
        }
    }

    @Override
    public FloatingView remove() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mEnFloatingView == null) {
                    return;
                }
                if (ViewCompat.isAttachedToWindow(mEnFloatingView) && mContainer != null) {
                    mContainer.removeView(mEnFloatingView);
                }
                mEnFloatingView = null;
            }
        });
        return this;
    }

    private void ensureMiniPlayer(Context context) {
        synchronized (this) {
            if (mEnFloatingView != null) {
                return;
            }
            mEnFloatingView = new EnFloatingView(context.getApplicationContext());
            mLogView = new LogView(context.getApplicationContext());
            mLogView.setLayoutParams(getLogParams());
            mLogView.setVisibility(View.GONE);
            mEnFloatingView.setLayoutParams(getParams(context));
            mEnFloatingView.setMagnetViewListener(new MagnetViewListener() {
                @Override
                public void onRemove(FloatingMagnetView magnetView) {

                }

                @Override
                public void onClick(FloatingMagnetView magnetView) {
                    // 每次点击都会变化日志状态
                    changeLogState();
                }
            });
            addViewToWindow(mEnFloatingView);
        }
    }


    private void changeLogState() {
        if (mLogView != null) {
            int i = mLogView.getVisibility();
            if (i == View.VISIBLE) {
                mLogView.setVisibility(View.GONE);
                isStopTimer = true;
            } else {
                isStopTimer = false;
                handler.sendMessage(handler.obtainMessage());
                mLogView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        final float rate = DeviceInfoManager.getCurProcessCpuRate();

        if (mLogView != null) {
            mLogView.post(new Runnable() {
                @Override
                public void run() {
                    String cpuInfo;

                    cpuInfo = String.format("[ CPU当前使用率: %s%s ]", String.valueOf((int) rate), "%");

                    mLogView.setTxCpuInfo(cpuInfo);
                }
            });
        }
        if (!isStopTimer) {
            handler.sendMessageDelayed(handler.obtainMessage(), 800);
        }
        return true;
    }


    private FrameLayout.LayoutParams getLogParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.TOP | Gravity.START;

        return params;
    }

    @Override
    public FloatingView add() {
        ensureMiniPlayer(EnContext.get());
        return this;
    }

    @Override
    public FloatingView attach(Activity activity) {
        attach(getActivityRoot(activity));
        return this;
    }

    @Override
    public FloatingView attach(FrameLayout container) {
        if (container == null || mEnFloatingView == null) {
            mContainer = container;
            return this;
        }
        if (mEnFloatingView.getParent() == container) {
            return this;
        }
        // 避免重复添加View，所以在这里需要remover掉
        if (mEnFloatingView != null && mEnFloatingView.getParent() != null) {
            FrameLayout frameLayout = (FrameLayout) mEnFloatingView.getParent();
            frameLayout.removeView(mEnFloatingView);
        }

        // 避免重复添加View，所以在这里需要remover掉
        if (mLogView != null && mLogView.getParent() != null) {
            FrameLayout frameLayout = (FrameLayout) mLogView.getParent();
            frameLayout.removeView(mLogView);
        }

        mContainer = container;

        container.addView(mLogView);
        container.addView(mEnFloatingView);

        return this;
    }

    @Override
    public FloatingView detach(Activity activity) {
        detach(getActivityRoot(activity));
        return this;
    }

    @Override
    public FloatingView detach(FrameLayout container) {
        if (mEnFloatingView != null && container != null && ViewCompat.isAttachedToWindow(mEnFloatingView)) {
            container.removeView(mEnFloatingView);
        }

        if (mLogView != null && container != null && ViewCompat.isAttachedToWindow(mLogView)) {
            container.removeView(mLogView);
        }

        if (mContainer == container) {
            mContainer = null;
        }

        return this;
    }

    @Override
    public EnFloatingView getView() {
        return mEnFloatingView;
    }

    public FloatingView layoutParams(ViewGroup.LayoutParams params) {
        if (mEnFloatingView != null) {
            mEnFloatingView.setLayoutParams(params);
        }
        return this;
    }

    private void addViewToWindow(final EnFloatingView view) {
        if (mContainer == null) {
            return;
        }
        mContainer.addView(view);
    }

    private FrameLayout.LayoutParams getParams(Context context) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.END;
        float dpDimension = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
        params.setMargins(13, params.topMargin, params.rightMargin, (int) dpDimension);
        return params;
    }

    private FrameLayout getActivityRoot(Activity activity) {
        if (activity == null) {
            return null;
        }
        try {
            return (FrameLayout) activity.getWindow().getDecorView().findViewById(android.R.id.content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}