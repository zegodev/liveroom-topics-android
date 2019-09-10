package com.zego.videoexternalrender.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import com.zego.videoexternalrender.R;
import com.zego.zegoavkit2.enums.VideoExternalRenderType;
import com.zego.zegoavkit2.videorender.VideoRenderType;
import com.zego.zegoavkit2.videorender.ZegoExternalVideoRender;

/**
 * 外部渲染返回视频数据的类型选择
 */
public class ZGVideoRenderTypeUI extends AppCompatActivity {

    private RadioGroup mRenderTypeGroup;
    private CheckBox mCheckBox;

    // 外部渲染类型
    private VideoRenderType renderType = VideoRenderType.VIDEO_RENDER_TYPE_NONE;

    // 是否已开启外部渲染
    private boolean isEnableExternalRender = false;
    private boolean isGivenDecodeCallback = false;

    // 加载c++ so
    static {
        System.loadLibrary("nativeCutPlane");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_render_type);

        mCheckBox = (CheckBox)findViewById(R.id.CheckboxNotDecode);
        mRenderTypeGroup = (RadioGroup)findViewById(R.id.RenderTypeGroup);
        final int[] radioRenderTypeBtns = {R.id.RadioRGB, R.id.RadioYUV, R.id.RadioANY};

        // 设置RadioGroup组件的事件监听
        mRenderTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedID) {

                if (radioRenderTypeBtns[0] == radioGroup.getCheckedRadioButtonId()) {
                    // 外部渲染时抛出rgb格式的视频数据，例如PIXEL_FORMAT_RGBA32,SDK内部不渲染
                    renderType = VideoRenderType.VIDEO_RENDER_TYPE_RGB;
                } else if (radioRenderTypeBtns[1] == radioGroup.getCheckedRadioButtonId()){
                    /**
                     * 外部渲染时抛出解码后的视频数据，例如PIXEL_FORMAT_I420、PIXEL_FORMAT_NV12、PIXEL_FORMAT_BGRA
                     * Android推流时返回PIXEL_FORMAT_RGBA32，拉流时返回PIXEL_FORMAT_I420，SDK 内部不渲染
                     */
                    renderType = VideoRenderType.VIDEO_RENDER_TYPE_YUV;
                } else {
                    // 外部渲染时抛出未解码的视频数据--CODEC_TYPE_AVC_ANNEXB，SDK 内部不渲染，推流时 SDK返回 PIXEL_FORMAT_RGBA32格式数据
                    renderType = VideoRenderType.VIDEO_RENDER_TYPE_ANY;
                }
                // 推流处开启外部采集功能
            }
        });

        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isGivenDecodeCallback = true;
                } else {
                    isGivenDecodeCallback = false;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 若开启过视频外部渲染，此处关闭
        if (isEnableExternalRender) {
            // 关闭外部渲染功能
            ZegoExternalVideoRender.setVideoRenderType(VideoRenderType.VIDEO_RENDER_TYPE_NONE);
        }
    }

    public void JumpPublish(View view){

        if (renderType != null){
            // 开启外部渲染功能
            ZegoExternalVideoRender.setVideoRenderType(renderType);
            isEnableExternalRender = true;
        }

        Intent intent = new Intent(ZGVideoRenderTypeUI.this, ZGVideoRenderUI.class);
        intent.putExtra("RenderType",renderType.value());
        intent.putExtra("IsUseNotDecode", isGivenDecodeCallback);
        ZGVideoRenderTypeUI.this.startActivity(intent);
    }
}
