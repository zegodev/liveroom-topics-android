package com.zego.common.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.zego.common.R;


public class TitleLayout extends FrameLayout {

    private TextView titleView;

    public TitleLayout(Context context) {
        this(context, null);
    }

    public TitleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TitleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View view = inflate(context, R.layout.activity_title, this);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.title);
        titleView = view.findViewById(R.id.txt_title);
        String title = typedArray.getString(R.styleable.title_name);
        titleView.setText(title);
    }

    @NonNull
    public void setTitleName(String name) {
        if (titleView != null) {
            titleView.setText(name);
        }
    }


}
