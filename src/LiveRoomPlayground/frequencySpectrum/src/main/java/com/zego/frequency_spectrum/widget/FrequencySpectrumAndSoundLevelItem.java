package com.zego.frequency_spectrum.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zego.frequency_spectrum.R;

public class FrequencySpectrumAndSoundLevelItem extends LinearLayout {

    private BeatLoadView beatLoadView;

    public BeatLoadView getBeatLoadView() {
        return beatLoadView;
    }



    private TextView tv_username;

    public TextView getTv_username() {
        return tv_username;
    }

    public void setTv_username(TextView tv_username) {
        this.tv_username = tv_username;
    }



    private String stream_id;

    public String getStream_id() {
        return stream_id;
    }

    public void setStream_id(String stream_id) {
        this.stream_id = stream_id;
    }



    private ProgressBar pb_play_sound_level;

    public ProgressBar getPb_play_sound_level() {
        return pb_play_sound_level;
    }

    public void setPb_play_sound_level(ProgressBar pb_play_sound_level) {
        this.pb_play_sound_level = pb_play_sound_level;
    }



    public FrequencySpectrumAndSoundLevelItem(Context ctx, AttributeSet attributeSet){

        super(ctx, attributeSet);

        LayoutInflater.from(ctx).inflate(R.layout.frequency_spectrum_sound_level_item, this);

        beatLoadView = findViewById(R.id.beat_load_view);

        tv_username = findViewById(R.id.tv_username);

        pb_play_sound_level = findViewById(R.id.pb_play_sound_level);


    }


}
