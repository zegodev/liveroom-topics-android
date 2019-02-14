package com.zego.videoexternalrender.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import com.zego.videoexternalrender.R;
import com.zego.zegoavkit2.enums.VideoExternalRenderType;
import com.zego.zegoavkit2.videorender.ZegoExternalVideoRender;

public class ZGVideoRenderTypeUI extends AppCompatActivity {

    private RadioGroup mRenderTypeGroup;

    private VideoExternalRenderType renderType;

    // 加载c++ so
    static {
        System.loadLibrary("nativeCutPlane");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_render_type);

        mRenderTypeGroup = (RadioGroup)findViewById(R.id.RenderTypeGroup);
        final int[] radioRenderTypeBtns = {R.id.RadioDecodeRGB, R.id.RadioDecode, R.id.RadioNotDecode};

        ZegoExternalVideoRender videoRender = new ZegoExternalVideoRender();

        mRenderTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedID) {

                if (radioRenderTypeBtns[0] == radioGroup.getCheckedRadioButtonId()) {
                    renderType = VideoExternalRenderType.DECODE_RGB_SERIES;
                } else if (radioRenderTypeBtns[1] == radioGroup.getCheckedRadioButtonId()){
                    renderType = VideoExternalRenderType.DECODE;
                } else {
                    renderType = VideoExternalRenderType.NOT_DECODE;
                }
                // 开启外部渲染
                ZegoExternalVideoRender.enableExternalRender(true, renderType);
            }
        });
    }

    public void JumpPublish(View view){
        Intent intent = new Intent(ZGVideoRenderTypeUI.this, ZGVideoRenderUI.class);
        ZGVideoRenderTypeUI.this.startActivity(intent);
    }
}
