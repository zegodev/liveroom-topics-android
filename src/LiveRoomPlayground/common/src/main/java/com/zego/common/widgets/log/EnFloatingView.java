package com.zego.common.widgets.log;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.zego.common.R;


public class EnFloatingView extends FloatingMagnetView {

    private long mLastTouchDownTime;
    private static final int TOUCH_TIME_THRESHOLD = 150;
    private final TextView mIcon;

    public EnFloatingView(@NonNull Context context) {
        super(context, null);
        inflate(context, R.layout.log_floating_view, this);
        mIcon = findViewById(R.id.icon);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (event != null) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mLastTouchDownTime = System.currentTimeMillis();
                    break;
                case MotionEvent.ACTION_UP:

                    break;
            }
        }
        return true;
    }


    protected boolean isOnClickEvent() {
        return System.currentTimeMillis() - mLastTouchDownTime < TOUCH_TIME_THRESHOLD;
    }

}
