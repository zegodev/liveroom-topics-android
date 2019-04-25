package com.zego.common.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.zego.common.R;
import com.zego.common.util.PreferenceUtil;

/**
 * CustomEditText
 * 自定义文本框
 * 主要用于输入数据保存
 * 当用户填写数据到 EditText 时会自动保存下来，下次界面启动时自动
 * 恢复成原有的参数
 */
public class CustomEditText extends android.support.v7.widget.AppCompatEditText {

    private String key = null;
    private TextWatcher textWatcher = null;

    public CustomEditText(Context context) {
        this(context, null);
    }

    public CustomEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public CustomEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.customWidgets);
        key = typedArray.getString(R.styleable.customWidgets_key);
        // 如果当前存在key，则查询本地是否存在数据
        if (key != null && key.length() > 0) {
            String value = PreferenceUtil.getInstance().getStringValue(key, "");
            setText(value);
        }

        super.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (textWatcher != null) {
                    textWatcher.beforeTextChanged(s, start, count, after);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (textWatcher != null) {
                    textWatcher.onTextChanged(s, start, before, count);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 监听到文本变化的时候，自动存储参数
                saveCurrentParameter();

                if (textWatcher != null) {
                    textWatcher.afterTextChanged(s);
                }
            }
        });
    }

    private void saveCurrentParameter(){
        String value = getText().toString();
        if (!"".equals(value)) {
            PreferenceUtil.getInstance().setStringValue(key, value);
        }
    }

    @Override
    public void addTextChangedListener(TextWatcher watcher) {
        this.textWatcher = watcher;
    }
}
