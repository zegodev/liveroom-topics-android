package com.zego.videocapture.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.zego.common.ui.BaseActivity;
import com.zego.videocapture.R;
import com.zego.videocapture.videocapture.VideoCaptureFactoryDemo;
import com.zego.zegoavkit2.ZegoExternalVideoCapture;
import com.zego.zegoavkit2.screencapture.ZegoScreenCaptureFactory;
import com.zego.zegoliveroom.constants.ZegoConstants;

@TargetApi(21)
public class ZGVideoCaptureOriginUI extends BaseActivity {

    private RadioGroup mCaptureTypeGroup;
    private VideoCaptureFactoryDemo.CaptureOrigin captureOrigin;

    private ZegoExternalVideoCapture videoCapture;
    private VideoCaptureFactoryDemo factory;
    private ZegoScreenCaptureFactory screenCaptureFactory;

    private static final int REQUEST_CODE = 1001;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zgvideo_capture_type);

        mCaptureTypeGroup = (RadioGroup)findViewById(R.id.CaptureTypeGroup);
        final int[] radioCaptureTypeBtns = {R.id.RadioImage, R.id.RadioScreen, R.id.RadioCamera, R.id.RadioCameraYUV, R.id.RadioCameraBitStream};

        videoCapture = new ZegoExternalVideoCapture();

        mCaptureTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedID) {

                if (radioCaptureTypeBtns[0] == radioGroup.getCheckedRadioButtonId()) {
                    captureOrigin = VideoCaptureFactoryDemo.CaptureOrigin.CaptureOrigin_Image; //图片
                } else if (radioCaptureTypeBtns[1] == radioGroup.getCheckedRadioButtonId()) {
                    captureOrigin = VideoCaptureFactoryDemo.CaptureOrigin.CaptureOrigin_Screen; //录屏
                    // 检测系统版本
                    if(Build.VERSION.SDK_INT < 21){
                        Toast.makeText(ZGVideoCaptureOriginUI.this, "录屏功能只能在Android5.0及以上版本的系统中运行", Toast.LENGTH_SHORT).show();
                        finish();
                    }else {

                        // 1. 请求录屏权限, 等待用户授权
                        mMediaProjectionManager =  (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
                    }

                } else if (radioCaptureTypeBtns[2] == radioGroup.getCheckedRadioButtonId()){
                    captureOrigin = VideoCaptureFactoryDemo.CaptureOrigin.CaptureOrigin_CameraV2; //摄像头
                } else if (radioCaptureTypeBtns[3] == radioGroup.getCheckedRadioButtonId()){
                    captureOrigin = VideoCaptureFactoryDemo.CaptureOrigin.CaptureOrigin_Camera; //摄像头 yuv数据
                } else {
                    captureOrigin = VideoCaptureFactoryDemo.CaptureOrigin.CaptureOrigin_CameraV3; //摄像头 码流数据
                }

                if (captureOrigin != VideoCaptureFactoryDemo.CaptureOrigin.CaptureOrigin_Screen){
                    // 创建工厂
                    factory = new VideoCaptureFactoryDemo(captureOrigin);
                    factory.setContext(ZGVideoCaptureOriginUI.this);

                    videoCapture.setVideoCaptureFactory(factory, ZegoConstants.PublishChannelIndex.MAIN);
                }
            }
        });
    }

    // 2.实现请求录屏权限结果通知接口
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d("Zego", "获取MediaProjection成功");

            //3.获取MediaProjection
            mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            //4.创建录屏工厂
            screenCaptureFactory = new ZegoScreenCaptureFactory();
            //5.设置MediaProjection
            screenCaptureFactory.setMediaProjection(mMediaProjection);

            //6.外部视频采集设置外部采集工厂
            videoCapture.setVideoCaptureFactory(screenCaptureFactory, ZegoConstants.PublishChannelIndex.MAIN);
        }
    }

    public void JumpPublish(View view){
        Intent intent = new Intent(ZGVideoCaptureOriginUI.this, ZGVideoCaptureDemoUI.class);
        boolean isScreen = (captureOrigin == VideoCaptureFactoryDemo.CaptureOrigin.CaptureOrigin_Screen)?true:false;
        intent.putExtra("IsScreenCapture",isScreen);
        ZGVideoCaptureOriginUI.this.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        videoCapture.setVideoCaptureFactory(null, ZegoConstants.PublishChannelIndex.MAIN);
        if (screenCaptureFactory != null){
            screenCaptureFactory.setMediaProjection(null);
        }
    }

}
