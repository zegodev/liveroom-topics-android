package com.zego.sound.processing.view;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;


import com.zego.sound.processing.R;
import com.zego.sound.processing.adapter.SoundEffectViewAdapter;

import java.util.ArrayList;
import java.util.List;

public class SoundEffectDialog extends Dialog implements TabLayout.OnTabSelectedListener {

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private SoundEffectViewAdapter mSoundEffectViewAdapter;

    private List<View> mTabViewList;

    private static final int TEXT_COLO_SELECTED = Color.parseColor("#0d70ff");
    private static final int TEXT_COLO_UNSELECTED = Color.parseColor("#333333");

    public SoundEffectDialog(@NonNull Context context) {
        super(context, R.style.CommonDialog);
        init();
        initView(context);
    }

    private void init() {
        mTabViewList = new ArrayList<>(3);
    }

    private void initView(Context context) {
        setContentView(R.layout.dialog_sound_effect_layout);
        // 设置可以取消
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        // 设置Dialog高度位置
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

        layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 600, context.getResources().getDisplayMetrics());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.gravity = Gravity.BOTTOM;
        // 设置没有边框
        getWindow().getDecorView().setPadding(0, 0, 0, 0);
        getWindow().setAttributes(layoutParams);

        // 初始化 音效 ViewPager
        mSoundEffectViewAdapter = new SoundEffectViewAdapter(context, getWindow());
        mViewPager = findViewById(R.id.view_pager);
        mViewPager.setAdapter(mSoundEffectViewAdapter);
        mTabLayout = findViewById(R.id.tab_layout);
        mTabLayout.addOnTabSelectedListener(this);
        // 添加Tab Item
        addTab(context, context.getResources().getString(R.string.voice_changer));
        addTab(context, context.getResources().getString(R.string.stereo));
        addTab(context, context.getResources().getString(R.string.mixed_voice));

        // 设置ViewPager pageChangeListener 使TabLayout 根据情况选择tab
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        // 设置ViewPager的缓存个数
        mViewPager.setOffscreenPageLimit(2);
        // 强制调用一次onTabSelected方法
        onTabSelected(mTabLayout.getTabAt(0));
    }

    public void setOnVoiceChangeListener(SoundEffectViewAdapter.OnVoiceChangeListener listener) {
        if (mSoundEffectViewAdapter != null) {
            mSoundEffectViewAdapter.setOnVoiceChangeListener(listener);

        }
    }

    public void setOnReverberationChangeListener(SoundEffectViewAdapter.OnReverberationChangeListener listener){
        if (mSoundEffectViewAdapter != null) {
            mSoundEffectViewAdapter.setOnReverberationChangeListener(listener);
        }
    }

    public void setOnStereoChangeListener(SoundEffectViewAdapter.OnStereoChangeListener listener){
        if (mSoundEffectViewAdapter != null) {
            mSoundEffectViewAdapter.setOnStereoChangeListener(listener);
        }
    }

    private void addTab(Context context, String tabText) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_item_layout, mTabLayout, false);

        ((TextView) view.findViewById(R.id.tab_item_text)).setText(tabText);
        mTabLayout.addTab(mTabLayout.newTab().setCustomView(view));

        mTabViewList.add(view);
    }

    private void setDialogHeight(int position) {
        // 设置Dialog高度位置
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        if (position == 0) {
            layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 360, this.getContext().getResources().getDisplayMetrics());
        } else if (position == 1) {
            layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 420, this.getContext().getResources().getDisplayMetrics());

        } else if (position == 2) {
            layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, this.getContext().getResources().getDisplayMetrics());
        }
        getWindow().setAttributes(layoutParams);

    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
        setDialogHeight(tab.getPosition());
        for (int i = 0; i < mTabViewList.size(); i++) {
            View view = mTabViewList.get(i);
            TextView text = view.findViewById(R.id.tab_item_text);
            View indicator = view.findViewById(R.id.tab_item_indicator);
            if (i == tab.getPosition()) { // 选中状态
                text.setTextColor(TEXT_COLO_SELECTED);
                indicator.setVisibility(View.VISIBLE);
            } else {// 未选中状态
                text.setTextColor(TEXT_COLO_UNSELECTED);
                indicator.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        // do nothing
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        // do nothing
    }

    public void setOnSoundEffectAuditionCheckedListener(SoundEffectViewAdapter.OnSoundEffectAuditionCheckedListener onSoundEffectAuditionCheckedListener) {
        mSoundEffectViewAdapter.setOnSoundEffectAuditionCheckedListener(onSoundEffectAuditionCheckedListener);
    }

    public void setOnSoundEffectChangedListener(SoundEffectViewAdapter.OnSoundEffectChangedListener onSoundEffectChangedListener) {
        mSoundEffectViewAdapter.setOnSoundEffectChangedListener(onSoundEffectChangedListener);
    }

    // 释放相关资源
    public void release() {
        mSoundEffectViewAdapter.release();
    }
}
