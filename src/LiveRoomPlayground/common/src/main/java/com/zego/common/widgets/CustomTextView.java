package com.zego.common.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.zego.common.R;

/**
 * Created by zego on 2019/3/21.
 */

public class CustomTextView extends android.support.v7.widget.AppCompatTextView {

    public CustomTextView(Context context) {
        this(context, null);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.customWidgets);

        boolean line = typedArray.getBoolean(R.styleable.customWidgets_line, false);
        if (line) {
            this.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        }
    }
}
