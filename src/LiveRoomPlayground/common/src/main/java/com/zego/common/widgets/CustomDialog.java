package com.zego.common.widgets;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.TextView;

import com.zego.common.R;

/**
 * Created by zego on 2019/4/16.
 */

public class CustomDialog extends Dialog {


    public CustomDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    private TextView textView;
    private String loadingText = "加载中";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading_dialog_view);
        // 设置空白处不能取消动画
        setCanceledOnTouchOutside(false);
        setCancelable(false);
        textView = findViewById(R.id.loading_text);
        textView.setText(loadingText);
    }

    private void setLoadingText(String loadingText) {
        this.loadingText = loadingText;
    }

    private static CustomDialog customDialog;

    @Override
    public void show() {
        synchronized (customDialog) {
            super.show();
        }
    }

    @Override
    public void cancel() {
        synchronized (customDialog) {
            super.cancel();
            customDialog = null;
        }
    }

    public static CustomDialog createDialog(Activity activity) {
        return createDialog("", activity);
    }

    /**
     * @param loadingText 加载文本
     */
    public static CustomDialog createDialog(String loadingText, Activity activity) {
        if (customDialog == null)

            synchronized (CustomDialog.class) {
                customDialog = new CustomDialog(activity, 0);
            }
        if (loadingText != null && !"".equals(loadingText)) {
            customDialog.setLoadingText(loadingText);
        }
        return customDialog;
    }
}
