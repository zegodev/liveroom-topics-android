package com.zego.mediarecorder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.ToggleButton;

public class ZGMediaRecorderSettingUI extends AppCompatActivity {

    private RadioGroup mRecordModeGroup;
    private RadioGroup mRecordFormatGroup;
//    private ToggleButton mPublishToggle;

    private boolean isUseFlv = true;
    private boolean isUsePublish = false;

    private int recordMode = 2; // both

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_avsetting);

//        mPublishToggle = (ToggleButton)findViewById(R.id.publish_tog);

        mRecordModeGroup = (RadioGroup)findViewById(R.id.RecordModeGroup);
        final int[] radioModeBtns = {R.id.RadioAudio, R.id.RadioVideo, R.id.RadioBoth};

        mRecordFormatGroup = (RadioGroup)findViewById(R.id.RecordFormatGroup);
        final int[] radioFormatBtns = {R.id.RadioFlv, R.id.RadioMp4};

        mRecordModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedID) {

                if (radioModeBtns[0] == radioGroup.getCheckedRadioButtonId()) {
                    recordMode = 0; //audio
                } else if (radioModeBtns[1] == radioGroup.getCheckedRadioButtonId()){
                    recordMode = 1; //video
                } else {
                    recordMode = 2; //both
                }
            }
        });

        mRecordFormatGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (radioFormatBtns[0] == radioGroup.getCheckedRadioButtonId()){
                    isUseFlv = true;
                } else {
                    isUseFlv = false;
                }
            }
        });

//        mPublishToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
//                if (isChecked) {
//                    mPublishToggle.setChecked(true);
//                    isUsePublish = true;
//                } else {
//                    mPublishToggle.setChecked(false);
//                    isUsePublish = false;
//                }
//            }
//        });
    }

    public void JumpRecording(View view) {
        Intent intent = new Intent(ZGMediaRecorderSettingUI.this, ZGMediaRecorderDemoUI.class);
        intent.putExtra("RecordMode",recordMode);
        intent.putExtra("RecordFormat", isUseFlv);
//        intent.putExtra("UsePublish", isUsePublish);

        ZGMediaRecorderSettingUI.this.startActivity(intent);
    }
}
