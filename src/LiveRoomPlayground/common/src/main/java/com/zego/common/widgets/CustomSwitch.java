package com.zego.common.widgets;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import com.zego.common.R;
import com.zego.common.util.PreferenceUtil;

/**
 * CustomSwitch
 * 自定义开关
 * 主要用于输入数据保存
 * 当用户修改Switch内容时 时会自动保存下来，下次界面启动时自动
 * 恢复成原有的参数
 */

public class CustomSwitch extends android.support.v7.widget.SwitchCompat implements CompoundButton.OnCheckedChangeListener {

    private String key = null;
    private OnCheckedChangeListener onCheckedChangeListener = null;

    public CustomSwitch(Context context) {
        this(context, null);
    }

    public CustomSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    @SuppressLint("NewApi")
    public CustomSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.customWidgets);
        key = typedArray.getString(R.styleable.customWidgets_key);
        // 如果当前存在key，则查询本地是否存在数据
        if (key != null && key.length() > 0) {
            boolean value = PreferenceUtil.getInstance().getBooleanValue(key, true);
            setChecked(value);
        }
        super.setOnCheckedChangeListener(this);
    }

    private void saveCurrentParameter() {
        boolean value = isChecked();
        if (!"".equals(value)) {
            PreferenceUtil.getInstance().setBooleanValue(key, value);
        }
    }

    @Override
    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        this.onCheckedChangeListener = listener;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (onCheckedChangeListener != null) {
            onCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
        }
        saveCurrentParameter();
    }
}
