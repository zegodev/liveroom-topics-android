package com.zego.videofilter.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.widget.Toast;

import com.zego.common.ZGManager;
import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.common.widgets.CustomDialog;
import com.zego.videofilter.R;
import com.zego.videofilter.ZGFilterHelper;
import com.zego.videofilter.databinding.ActivityFuBaseBinding;
import com.zego.videofilter.faceunity.FURenderer;
import com.zego.videofilter.videoFilter.VideoFilterFactoryDemo;
import com.zego.videofilter.view.BeautyControlView;
import com.zego.zegoavkit2.videofilter.ZegoExternalVideoFilter;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;


/**
 * 带美颜的推流界面
 *
 */

public class FUBeautyActivity extends AppCompatActivity implements FURenderer.OnTrackingStatusChangedListener {
    public final static String TAG = FUBeautyActivity.class.getSimpleName();

    private ActivityFuBaseBinding binding;

    private ViewStub mBottomViewStub;
    private BeautyControlView mBeautyControlView;

    // faceunity 美颜相关的封装类
    protected FURenderer mFURenderer;

    // 房间 ID
    private String mRoomID = "";

    // 主播流名
    private String anchorStreamID = ZegoUtil.getPublishStreamID();

    private VideoFilterFactoryDemo.FilterType chooseFilterType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_fu_base);

        mBottomViewStub = (ViewStub) findViewById(R.id.fu_base_bottom);
        mBottomViewStub.setInflatedId(R.id.fu_base_bottom);

        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 创建 faceUnity 美颜实例
        mFURenderer = new FURenderer
                .Builder(this)
                .maxFaces(4)
                .inputTextureType(0)
                .setOnTrackingStatusChangedListener(this)
                .build();

        mBottomViewStub.setLayoutResource(R.layout.layout_fu_beauty);
        mBottomViewStub.inflate();

        mBeautyControlView = (BeautyControlView) findViewById(R.id.fu_beauty_control);

        mRoomID = getIntent().getStringExtra("roomID");
        chooseFilterType = (VideoFilterFactoryDemo.FilterType)getIntent().getSerializableExtra ("FilterType");

        mBeautyControlView.setOnFUControlListener(mFURenderer);

        // 初始化SDK
        initSDK();

        // 设置 SDK 推流回调监听
        initSDKCallback();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mBeautyControlView.isShown()) {
            mBeautyControlView.hideBottomLayoutAnimator();
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBeautyControlView != null) {
            mBeautyControlView.onResume();
        }
    }

    @Override
    public void finish() {
        super.finish();

        // 在退出页面时停止推流
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().stopPublishing();
        // 停止预览
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().stopPreview();

        // 登出房间
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().logoutRoom();

        // 去除外部滤镜的设置
        ZegoExternalVideoFilter.setVideoFilterFactory(null, ZegoConstants.PublishChannelIndex.MAIN);

        // 释放 SDK
        ZGFilterHelper.sharedInstance().releaseZegoLiveRoom();
    }

    /**
     * 供其他Activity调用，进入本专题的方法
     *
     * @param activity
     */
    public static void actionStart(Activity activity, String roomID, VideoFilterFactoryDemo.FilterType filterType) {
        Intent intent = new Intent(activity, FUBeautyActivity.class);
        intent.putExtra("roomID",roomID);
        intent.putExtra("FilterType", filterType);
        activity.startActivity(intent);
    }

    /**
     * 初始化SDK逻辑
     * 初始化成功后登录房间并推流
     */
    private void initSDK() {
        AppLogger.getInstance().i(VideoFilterMainUI.class, "初始化ZEGO SDK");

        /**
         * 需要在 initSDK 之前设置 SDK 环境，此处设置为测试环境；
         * 从官网申请的 AppID 默认是测试环境，而 SDK 初始化默认是正式环境，所以需要在初始化 SDK 前设置测试环境，否则 SDK 会初始化失败；
         * 当 App 集成完成后，再向 ZEGO 申请开启正式环境，改为正式环境再初始化。
         */
        ZegoLiveRoom.setTestEnv(true);

        // 设置外部滤镜---必须在初始化 ZEGO SDK 之前设置，否则不会回调   SyncTexture
        VideoFilterFactoryDemo filterFactory = new VideoFilterFactoryDemo(chooseFilterType, mFURenderer);
        ZegoExternalVideoFilter.setVideoFilterFactory(filterFactory, ZegoConstants.PublishChannelIndex.MAIN);

        // 初始化SDK
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().initSDK(ZGManager.appId, ZGManager.appSign, new IZegoInitSDKCompletionCallback() {
            @Override
            public void onInitSDK(int errorCode) {

                if (errorCode == 0) {
                    // 初始化成功，登录房间并推流
                    startPublish();

                    AppLogger.getInstance().i(FUBeautyActivity.class, "初始化ZEGO SDK成功");
                } else {
                    Toast.makeText(FUBeautyActivity.this, getString(com.zego.common.R.string.tx_init_failure), Toast.LENGTH_SHORT).show();
                    AppLogger.getInstance().i(FUBeautyActivity.class, "初始化ZEGO SDK失败 errorCode : %d", errorCode);
                }
            }
        });
    }

    public void startPublish(){

        // 防止用户点击，弹出加载对话框
        CustomDialog.createDialog("登录房间中...", this).show();
        AppLogger.getInstance().i(FUBeautyActivity.class, getString(R.string.tx_login_room));

        // 开始推流前需要先登录房间，此处是主播登录房间，成功登录后开始推流
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().loginRoom(mRoomID, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                CustomDialog.createDialog(FUBeautyActivity.this).cancel();
                if (errorCode == 0) {
                    AppLogger.getInstance().i(FUBeautyActivity.class, "登录房间成功 roomId : %s", mRoomID);

                    // 设置预览视图模式，此处采用 SDK 默认值--等比缩放填充整View，可能有部分被裁减。
                    ZGFilterHelper.sharedInstance().getZegoLiveRoom().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                    // 设置预览 view，主播自己推流采用全屏视图
                    ZGFilterHelper.sharedInstance().getZegoLiveRoom().setPreviewView(binding.preview);
                    // 启动预览
                    ZGFilterHelper.sharedInstance().getZegoLiveRoom().startPreview();

                    // 开始推流，flag 使用连麦场景，推荐场景
                    ZGFilterHelper.sharedInstance().getZegoLiveRoom().startPublishing(anchorStreamID, "anchor", ZegoConstants.PublishFlag.JoinPublish);

                } else {
                    AppLogger.getInstance().i(FUBeautyActivity.class, "登录房间失败, errorCode : %d", errorCode);
                    Toast.makeText(FUBeautyActivity.this, getString(com.zego.common.R.string.tx_login_room_failure), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 设置 SDK 推流回调监听
    private void initSDKCallback(){
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().setZegoLivePublisherCallback(new IZegoLivePublisherCallback() {
            @Override
            public void onPublishStateUpdate(int errorCode, String streamID, HashMap<String, Object> hashMap) {
                // 推流状态更新，errorCode 非0 则说明推流失败
                // 推流常见错误码请看文档: <a>https://doc.zego.im/CN/308.html</a>

                if (errorCode == 0) {
                    AppLogger.getInstance().i(FUBeautyActivity.class, "推流成功, streamID : %s", streamID);
                    Toast.makeText(FUBeautyActivity.this, getString(R.string.tx_publish_success), Toast.LENGTH_SHORT).show();
                } else {
                    AppLogger.getInstance().i(FUBeautyActivity.class, "推流失败, streamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(FUBeautyActivity.this, getString(R.string.tx_publish_fail), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onJoinLiveRequest(int i, String s, String s1, String s2) {

            }

            @Override
            public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {

            }

            @Override
            public AuxData onAuxCallback(int i) {
                return null;
            }

            @Override
            public void onCaptureVideoSizeChangedTo(int i, int i1) {

            }

            @Override
            public void onMixStreamConfigUpdate(int i, String s, HashMap<String, Object> hashMap) {

            }

            @Override
            public void onCaptureVideoFirstFrame() {

            }

            @Override
            public void onCaptureAudioFirstFrame() {

            }
        });
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~FURenderer信息回调~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onTrackingStatusChanged(final int status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // faceunity 是否检测到人脸的通知
                binding.fuBaseIsTrackingText.setVisibility(status > 0 ? View.INVISIBLE : View.VISIBLE);
            }
        });
    }
}
