package com.zego.common.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zego.common.R;


public class CustomMinSeekBar extends RelativeLayout {

    private Float min = 0.0f;
    private Float max = 1.0f;
    private String title = "";
    private TextView titleView, minView, maxView, value;
    private View buttonDescribe;
    private SeekBar seekBar;
    private Float currentProgress = 0.0f;
    private Context context;

    public CustomMinSeekBar(Context context) {
        this(context, null);
    }

    public CustomMinSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomMinSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        this.context = context;
        @SuppressLint("CustomViewStyleable")
        TypedArray typedArray = context.obtainStyledAttributes(attrs, com.zego.common.R.styleable.customSeekBar);
        View view = inflate(context, R.layout.custom_seek_bar_layout, this);
        String title = typedArray.getString(R.styleable.customSeekBar_title);
        String minStr = typedArray.getString(R.styleable.customSeekBar_minStr);
        String maxStr = typedArray.getString(R.styleable.customSeekBar_maxStr);
        final String describe = typedArray.getString(R.styleable.customSeekBar_describe);
        final String currentProgressStr = typedArray.getString(R.styleable.customSeekBar_currentStr);
        seekBar = view.findViewById(R.id.seek_bar);
        titleView = view.findViewById(R.id.title);
        value = view.findViewById(R.id.value);
        buttonDescribe = view.findViewById(R.id.button_describe);

        if (title != null && !"".equals(title)) {
            titleView.setText(title);
        }

        if ("".equals(describe) || describe == null) {
            buttonDescribe.setVisibility(GONE);
        }

        float difference = 0.0f;

        minView = view.findViewById(R.id.min);
        if (minStr != null && !"".equals(minStr)) {
            min = Float.valueOf(minStr);
            if (min < 0.0) {
                difference = Math.abs(min);
            }
            minView.setText(minStr);
        }

        maxView = view.findViewById(R.id.max);
        if (maxStr != null && !"".equals(maxStr)) {
            max = Float.valueOf(maxStr);
            maxView.setText(maxStr);
            seekBar.setMax(((int) (difference + max) * 10));
        }


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentProgress = (progress - ((0 - min) * 10)) / 10;
                value.setText(String.valueOf(currentProgress));
                if (seekBarChangeListener != null) {
                    seekBarChangeListener.onProgressChanged(seekBar, currentProgress, fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (seekBarChangeListener != null) {
                    seekBarChangeListener.onStartTrackingTouch(seekBar, currentProgress);
                }

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBarChangeListener != null) {
                    seekBarChangeListener.onStopTrackingTouch(seekBar, currentProgress);
                }
            }
        });


        setCurrentValue(Float.valueOf(currentProgressStr));

        buttonDescribe.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopWindows(describe, v);
            }
        });
    }

    public interface OnSeekBarChangeListener {
        void onProgressChanged(SeekBar seekBar, float progress, boolean fromUser);

        void onStartTrackingTouch(SeekBar seekBar, float progress);

        void onStopTrackingTouch(SeekBar seekBar, float progress);
    }

    private OnSeekBarChangeListener seekBarChangeListener;

    public synchronized void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.seekBarChangeListener = listener;
    }

    /**
     * 显示描述窗口
     *
     * @param msg  显示内容
     * @param view
     */
    private void showPopWindows(String msg, View view) {
        //创建并显示popWindow
        new CustomPopWindow.PopupWindowBuilder(context)
                .enableBackgroundDark(false) //弹出popWindow时，背景是否变暗
                .setBgDarkAlpha(0.7f) // 控制亮度
                .create()
                .setMsg(msg)
                .setMsgColor("#333333")
                .showAsDropDown(view);
    }

    public void setCurrentValue(float currentProgress) {
        this.currentProgress = currentProgress;
        float difference = 0.0f;
        if (min < 0.0) {
            difference = Math.abs(min);
        }
        seekBar.setProgress((int) ((currentProgress + difference) * 10));

    }

    public float getCurrentValue() {
        return currentProgress;
    }
}