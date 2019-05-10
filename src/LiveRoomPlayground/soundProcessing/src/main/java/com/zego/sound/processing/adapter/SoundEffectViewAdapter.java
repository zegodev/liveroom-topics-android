package com.zego.sound.processing.adapter;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.zego.common.widgets.ArcSeekBar;
import com.zego.common.widgets.CustomMinSeekBar;
import com.zego.common.widgets.RelativeRadioGroup;
import com.zego.sound.processing.R;
import com.zego.sound.processing.ZGSoundProcessingHelper;
import com.zego.zegoavkit2.audioprocessing.ZegoAudioReverbMode;

import java.util.ArrayList;
import java.util.List;

/**
 * 音效PagerAdapter，创建的View不能被回收，因此需在ViewPager中进行 setOffscreenPageLimit()设置
 */
public class SoundEffectViewAdapter extends PagerAdapter implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    // 音效设置View的数量
    private final static int VIEW_COUNT = 3;

    private final static int TEXT_COLOR_SELECTED = Color.parseColor("#0d70ff");
    private final static int TEXT_COLOR_UNSELECTED = Color.parseColor("#333333");


    // View的分组
    private final static int VIEW_GROUP_VOICE_CHANGE = 0x10;  // 变声

    // checkBox 列表
    private List<CheckBox> checkBoxList;
    // 当前checkBox的状态
    private boolean currentCheckBoxState;

    private List<TextView> voiceChangeTextViewList;
    private List<TextView> stereoTextViewList;
    private List<TextView> mixedTextViewList;

    private OnSoundEffectChangedListener onSoundEffectChangedListener;
    private OnSoundEffectAuditionCheckedListener onSoundEffectAuditionCheckedListener;

    private Context context;
    private AudioManager audioManager;
    private BroadcastReceiver headSetBroadcastReceiver;
    private Window window;

    public SoundEffectViewAdapter(Context context, Window window) {
        checkBoxList = new ArrayList<>(3);
        currentCheckBoxState = false;
        this.window = window;
        voiceChangeTextViewList = new ArrayList<>(3);
        stereoTextViewList = new ArrayList<>(3);
        mixedTextViewList = new ArrayList<>(3);

        // 初始化耳机相关监听
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        headSetBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null && action.equals(Intent.ACTION_HEADSET_PLUG)) {
                    if (intent.hasExtra("state")) {
                        int state = intent.getIntExtra("state", -1);
                        //  耳机 拔出
                        if (state == 0) {
                            // 当勾选的情况
                            if (currentCheckBoxState && checkBoxList != null && !checkBoxList.isEmpty()) {
                                // 切换状态
                                checkBoxList.get(0).toggle();
                            }
                        } else if (state == 1) {
                            // DO NOTHING
                        }
                    }
                }
            }
        };
        this.context = context;
        context.registerReceiver(headSetBroadcastReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public int getCount() {
        return VIEW_COUNT;
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        TextView textView;
        CheckBox checkBox = null;
        View view = null;
        // 变声
        if (position == 0) {
            view = LayoutInflater.from(container.getContext()).inflate(R.layout.voice_change_item_layout, container, false);
            final RadioGroup radioGroup = view.findViewById(R.id.rg_voice);
            checkBox = view.findViewById(R.id.checkbox);
            final CustomMinSeekBar customMinSeekBar = view.findViewById(R.id.tones);
            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == R.id.no) {
                        customMinSeekBar.setCurrentValue(ZGSoundProcessingHelper.VOICE_CHANGE_NO);
                        if (onVoiceChangeListener != null) {
                            onVoiceChangeListener.onVoiceChangeParam(ZGSoundProcessingHelper.VOICE_CHANGE_NO);
                        }
                    } else if (checkedId == R.id.loli) {
                        customMinSeekBar.setCurrentValue(ZGSoundProcessingHelper.VOICE_CHANGE_LOLI);
                        if (onVoiceChangeListener != null) {
                            onVoiceChangeListener.onVoiceChangeParam(ZGSoundProcessingHelper.VOICE_CHANGE_LOLI);
                        }
                    } else if (checkedId == R.id.uncle) {
                        customMinSeekBar.setCurrentValue(ZGSoundProcessingHelper.VOICE_CHANGE_UNCLE);
                        if (onVoiceChangeListener != null) {
                            onVoiceChangeListener.onVoiceChangeParam(ZGSoundProcessingHelper.VOICE_CHANGE_UNCLE);
                        }
                    }
                }
            });

            customMinSeekBar.setOnSeekBarChangeListener(new CustomMinSeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, float progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar, float progress) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar, float progress) {
                    if (progress == ZGSoundProcessingHelper.VOICE_CHANGE_NO) {
                        radioGroup.check(R.id.no);
                    } else if (progress == ZGSoundProcessingHelper.VOICE_CHANGE_LOLI) {
                        radioGroup.check(R.id.loli);
                    } else if (progress == ZGSoundProcessingHelper.VOICE_CHANGE_UNCLE) {
                        radioGroup.check(R.id.uncle);
                    } else {
                        radioGroup.check(R.id.custom);
                    }
                    if (onVoiceChangeListener != null) {
                        onVoiceChangeListener.onVoiceChangeParam(progress);
                    }
                }
            });

        } else if (position == 1) {
            // 立体声
            view = LayoutInflater.from(container.getContext()).inflate(R.layout.stereo_item_layout, container, false);
            ArcSeekBar arcSeekBar = view.findViewById(R.id.angle_seek_bar);
            final TextView angle = view.findViewById(R.id.angle);
            checkBox = view.findViewById(R.id.checkbox);
            final String txAngle = context.getString(R.string.tx_angle);
            arcSeekBar.setOnProgressChangeListener(new ArcSeekBar.OnProgressChangeListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onProgressChanged(ArcSeekBar seekBar, int progress, boolean isUser) {

                    angle.setText(txAngle + seekBar.getProgress() + " °");
                }

                @Override
                public void onStartTrackingTouch(ArcSeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(ArcSeekBar seekBar) {
                    if (onStereoChangeListener != null) {
                        onStereoChangeListener.onStereoChangeParam(seekBar.getProgress());
                    }
                }
            });
            // 默认90°
            arcSeekBar.setProgress(90);
        } else if (position == 2) {
            // 混响
            view = LayoutInflater.from(container.getContext()).inflate(R.layout.reverb_item_layout, container, false);
            final CustomMinSeekBar roomSize = view.findViewById(R.id.room_size);
            final RelativeRadioGroup relativeRadioGroup = view.findViewById(R.id.rg_reverb);
            checkBox = view.findViewById(R.id.checkbox);
            roomSize.setOnSeekBarChangeListener(new CustomMinSeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, float progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar, float progress) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar, float progress) {
                    if (onReverberationChangeListener != null) {
                        onReverberationChangeListener.onRoomSizeChange(progress);
                    }
                    if (relativeRadioGroup != null && relativeRadioGroup.getCheckedRadioButtonId() != R.id.custom) {
                        relativeRadioGroup.check(R.id.custom);
                    }
                }
            });

            final CustomMinSeekBar dryWetRatio = view.findViewById(R.id.dry_wet_ratio);
            dryWetRatio.setOnSeekBarChangeListener(new CustomMinSeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, float progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar, float progress) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar, float progress) {
                    if (onReverberationChangeListener != null) {
                        onReverberationChangeListener.onDryWetRationChange(progress);
                    }
                    if (relativeRadioGroup != null && relativeRadioGroup.getCheckedRadioButtonId() != R.id.custom) {
                        relativeRadioGroup.check(R.id.custom);
                    }
                }
            });

            final CustomMinSeekBar damping = view.findViewById(R.id.damping);
            damping.setOnSeekBarChangeListener(new CustomMinSeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, float progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar, float progress) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar, float progress) {
                    if (onReverberationChangeListener != null) {
                        onReverberationChangeListener.onDamping(progress);
                    }
                    if (relativeRadioGroup != null && relativeRadioGroup.getCheckedRadioButtonId() != R.id.custom) {
                        relativeRadioGroup.check(R.id.custom);
                    }
                }
            });

            final CustomMinSeekBar reverberance = view.findViewById(R.id.reverberance);
            reverberance.setOnSeekBarChangeListener(new CustomMinSeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, float progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar, float progress) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar, float progress) {
                    if (onReverberationChangeListener != null) {
                        onReverberationChangeListener.onReverberance(progress);
                    }
                    if (relativeRadioGroup != null && relativeRadioGroup.getCheckedRadioButtonId() != R.id.custom) {
                        relativeRadioGroup.check(R.id.custom);
                    }
                }
            });

            relativeRadioGroup.setOnCheckedChangeListener(new RelativeRadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RelativeRadioGroup group, int checkedId) {
                    if (checkedId == R.id.no) {
                        if (onReverberationChangeListener != null) {
                            onReverberationChangeListener.onAudioReverbModeChange(false, ZegoAudioReverbMode.CONCERT_HALL);
                        }
                    } else if (checkedId == R.id.concert_hall) {
                        if (onReverberationChangeListener != null) {
                            onReverberationChangeListener.onAudioReverbModeChange(true, ZegoAudioReverbMode.CONCERT_HALL);
                        }
                    } else if (checkedId == R.id.large_auditorium) {
                        if (onReverberationChangeListener != null) {
                            onReverberationChangeListener.onAudioReverbModeChange(true, ZegoAudioReverbMode.LARGE_AUDITORIUM);
                        }
                    } else if (checkedId == R.id.warm_club) {
                        if (onReverberationChangeListener != null) {
                            onReverberationChangeListener.onAudioReverbModeChange(true, ZegoAudioReverbMode.WARM_CLUB);
                        }
                    } else if (checkedId == R.id.soft_room) {
                        if (onReverberationChangeListener != null) {
                            onReverberationChangeListener.onAudioReverbModeChange(true, ZegoAudioReverbMode.SOFT_ROOM);
                        }
                    } else if (checkedId == R.id.custom) {
                        if (onReverberationChangeListener != null) {
                            onReverberationChangeListener.onRoomSizeChange(roomSize.getCurrentValue());
                            onReverberationChangeListener.onDamping(damping.getCurrentValue());
                            onReverberationChangeListener.onReverberance(reverberance.getCurrentValue());
                            onReverberationChangeListener.onDryWetRationChange(dryWetRatio.getCurrentValue());
                        }
                    }
                }
            });

        }

        checkBox.setChecked(currentCheckBoxState);
        checkBox.setOnCheckedChangeListener(this);
        // 试听文案添加点击事件监听
        view.findViewById(R.id.audition_layout).setOnClickListener(this);
        checkBoxList.add(checkBox);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // do nothing
    }

    /**
     * 释放相关资源
     */
    public void release() {
        audioManager = null;
        context.registerReceiver(headSetBroadcastReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.audition_layout) {
            CheckBox checkBox = ((ViewGroup) v).findViewById(R.id.checkbox);
            if (checkBox != null) {
                checkAndToggle(checkBox);
            }
            return;
        }
        // 如果是非选中，才进行处理
        if (!(boolean) v.getTag(R.id.sound_effect_selected_state)) {
            // 设置选中的字体颜色
            ((TextView) v).setTextColor(TEXT_COLOR_SELECTED);
            // 设置选中的图片背景
            setDrawableTopLevel((TextView) v, 2);
            // 设置选中状态
            v.setTag(R.id.sound_effect_selected_state, true);
            // 变声组
            if ((int) v.getTag(R.id.sound_effect_view_group_type) == VIEW_GROUP_VOICE_CHANGE) {
                for (TextView textView : voiceChangeTextViewList) {
                    // 重置其他没有选中的textView的状态
                    if (textView != v) {
                        // 设置字体颜色
                        textView.setTextColor(TEXT_COLOR_UNSELECTED);
                        setDrawableTopLevel(textView, 1);
                        textView.setTag(R.id.sound_effect_selected_state, false);
                    }
                }
            }

            // 进行音效状态改变回调
            if (onSoundEffectChangedListener != null) {
                onSoundEffectChangedListener.onSoundEffectChanged((int) v.getTag(R.id.sound_effect_type));
            }
        }
    }

    /**
     * 检查后 checkbox 切换状态
     *
     * @param checkBox 检查和切换状态的 checkbox
     */
    private void checkAndToggle(CheckBox checkBox) {
        if (!checkBox.isChecked()) {
            // 如果没有勾选，并且没有插耳机，提示
            if (!audioManager.isWiredHeadsetOn()) {
                Toast.makeText(context, "音效试听需要带上耳机才可使用", Toast.LENGTH_LONG).show();
            } else {
                // 否则，切换状态
                checkBox.toggle();
            }
        } else {
            checkBox.toggle();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        currentCheckBoxState = isChecked;

        for (CheckBox checkBox : checkBoxList) {
            // 对其他对checkBox进行处理
            if (checkBox != buttonView) {
                // 先置null，避免重复调用
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(isChecked);
                checkBox.setOnCheckedChangeListener(this);
            }
        }

        if (onSoundEffectAuditionCheckedListener != null) {
            onSoundEffectAuditionCheckedListener.onSoundEffectAuditionChecked(isChecked);
        }
    }

    /**
     * 设置check点击回调事件
     */
    public void setOnSoundEffectAuditionCheckedListener(OnSoundEffectAuditionCheckedListener onSoundEffectAuditionCheckedListener) {
        this.onSoundEffectAuditionCheckedListener = onSoundEffectAuditionCheckedListener;
    }

    /**
     * 设置音效状态改变事件
     */
    public void setOnSoundEffectChangedListener(OnSoundEffectChangedListener onSoundEffectChangedListener) {
        this.onSoundEffectChangedListener = onSoundEffectChangedListener;
    }

    // 设置 textView drawableTop level 值
    private void setDrawableTopLevel(TextView textView, int level) {
        if (textView != null && textView.getCompoundDrawables()[1] != null) {
            textView.getCompoundDrawables()[1].setLevel(level);
        }
    }

    /**
     * 音效改变监听器
     */
    public interface OnSoundEffectChangedListener {

        /**
         * 音效类型改变回调
         *
         * @param soundEffectType 音效类型
         */
        void onSoundEffectChanged(int soundEffectType);
    }

    /**
     * 音效试听点击回调
     */
    public interface OnSoundEffectAuditionCheckedListener {
        void onSoundEffectAuditionChecked(boolean isChecked);
    }

    private OnVoiceChangeListener onVoiceChangeListener;
    private OnStereoChangeListener onStereoChangeListener;
    private OnReverberationChangeListener onReverberationChangeListener;

    public void setOnVoiceChangeListener(OnVoiceChangeListener listener) {
        this.onVoiceChangeListener = listener;
    }

    public void setOnStereoChangeListener(OnStereoChangeListener listener) {
        this.onStereoChangeListener = listener;
    }

    public void setOnReverberationChangeListener(OnReverberationChangeListener listener) {
        this.onReverberationChangeListener = listener;
    }

    /**
     * 声音变化监听
     */
    public interface OnVoiceChangeListener {
        void onVoiceChangeParam(float param);
    }


    /**
     * 立体声变化监听
     */
    public interface OnStereoChangeListener {
        void onStereoChangeParam(int param);
    }

    /**
     * 混响参数变化监听器
     */
    public interface OnReverberationChangeListener {
        void onAudioReverbModeChange(boolean enable, ZegoAudioReverbMode mode);

        void onRoomSizeChange(float param);

        void onDryWetRationChange(float param);

        void onDamping(float param);

        void onReverberance(float param);
    }


}
