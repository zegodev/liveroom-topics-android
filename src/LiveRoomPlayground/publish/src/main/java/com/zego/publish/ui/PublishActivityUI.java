package com.zego.publish.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.zego.common.ZGPlayHelper;
import com.zego.common.application.ZegoApplication;
import com.zego.common.entity.SDKConfigInfo;
import com.zego.common.entity.StreamQuality;
import com.zego.publish.R;
import com.zego.publish.databinding.ActivityPublishBinding;
import com.zego.publish.databinding.PublishInputStreamIdLayoutBinding;
import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGConfigHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.constants.ZGLiveRoomConstants;
import com.zego.common.ui.BaseActivity;
import com.zego.common.ui.WebActivity;
import com.zego.common.util.AppLogger;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoResponseCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;

public class PublishActivityUI extends BaseActivity {


    private ActivityPublishBinding binding;
    private PublishInputStreamIdLayoutBinding layoutBinding;
    private StreamQuality streamQuality = new StreamQuality();
    private SDKConfigInfo sdkConfigInfo = new SDKConfigInfo();

    private String streamID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_publish);

        // 利用DataBinding 可以通过bean类驱动UI变化。
        // 方便快捷避免需要写一大堆 setText 等一大堆臃肿的代码。
        binding.setQuality(streamQuality);
        binding.setConfig(sdkConfigInfo);
        binding.swMic.setChecked(true);
        binding.swCamera.setChecked(true);
        layoutBinding = binding.layout;
        layoutBinding.startButton.setText(getString(R.string.tx_start_publish));

        streamQuality.setRoomID(String.format("RoomID : %s", getIntent().getStringExtra("roomID")));

        // 调用sdk 开始预览接口 设置view 启用预览
        ZGPublishHelper.sharedInstance().startPreview(binding.preview);
        // 设置推流回调
        ZGPublishHelper.sharedInstance().setPublisherCallback(new IZegoLivePublisherCallback() {

            // 推流回调文档说明: <a>https://doc.zego.im/API/ZegoLiveRoom/Android/html/index.html</a>
            @Override
            public void onPublishStateUpdate(int errorCode, String streamID, HashMap<String, Object> hashMap) {
                // 推流状态更新，errorCode 非0 则说明推流失败
                // 推流常见错误码请看文档: <a>https://doc.zego.im/CN/308.html</a>

                if (errorCode == 0) {
                    binding.title.setTitleName(getString(R.string.tx_publish_success));
                    AppLogger.getInstance().i(ZGPublishHelper.class, "推流成功, streamID : %s", streamID);
                    Toast.makeText(PublishActivityUI.this, getString(R.string.tx_publish_success), Toast.LENGTH_SHORT).show();
                } else {
                    binding.title.setTitleName(getString(R.string.tx_publish_fail));
                    AppLogger.getInstance().i(ZGPublishHelper.class, "推流失败, streamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(PublishActivityUI.this, getString(R.string.tx_publish_fail), Toast.LENGTH_SHORT).show();
                    // 当推流失败时需要显示布局
                    showInputStreamIDLayout();
                }

            }

            @Override
            public void onJoinLiveRequest(int i, String s, String s1, String s2) {
                /**
                 * 房间内有人申请加入连麦时会回调该方法
                 * 观众端可通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#requestJoinLive(IZegoResponseCallback)}
                 *  方法申请加入连麦
                 * **/
            }

            @Override
            public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {
                /**
                 * 推流质量更新, 回调频率默认3秒一次
                 * 可通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#setPublishQualityMonitorCycle(long)} 修改回调频率
                 */
                streamQuality.setFps(String.format("帧率: %f", zegoPublishStreamQuality.vnetFps));
                streamQuality.setBitrate(String.format("码率: %f kbs", zegoPublishStreamQuality.vkbps));
            }

            @Override
            public void onCaptureVideoSizeChangedTo(int width, int height) {
                // 当采集时分辨率有变化时，sdk会回调该方法
                streamQuality.setResolution(String.format("分辨率: %dX%d", width, height));
            }

            @Override
            public void onCaptureVideoFirstFrame() {
                // 当SDK采集摄像头捕获到第一帧时会回调该方法

            }

            @Override
            public void onCaptureAudioFirstFrame() {
                // 当SDK音频采集设备捕获到第一帧时会回调该方法
            }
        });


        // 监听摄像头与麦克风开关
        binding.swCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableCamera(isChecked);
                    ZGConfigHelper.sharedInstance().enableCamera(isChecked);
                }
            }
        });

        binding.swMic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableMic(isChecked);
                    ZGConfigHelper.sharedInstance().enableMic(isChecked);
                }

            }
        });

        // 设置SDK 房间代理回调。业务侧希望检查当前房间有流更新了，会去自动重新拉流。
        ZGBaseHelper.sharedInstance().setZegoRoomCallback(new IZegoRoomCallback() {
            @Override
            public void onKickOut(int reason, String roomID, String customReason) {

            }

            @Override
            public void onDisconnect(int i, String s) {
                binding.title.setTitleName("房间与server断开连接");
            }

            @Override
            public void onReconnect(int i, String s) {

            }

            @Override
            public void onTempBroken(int i, String s) {

            }

            @Override
            public void onStreamUpdated(int type, ZegoStreamInfo[] zegoStreamInfos, String s) {

            }

            @Override
            public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String s) {

            }

            @Override
            public void onRecvCustomCommand(String s, String s1, String s2, String s3) {


            }
        });

    }


    @Override
    protected void onResume() {
        //  某些华为手机上，应用在后台超过2分钟左右，华为系统会把摄像头资源给释放掉，并且可能会断开你应用的网络连接,
        //  需要做前台服务来避免这种情况https://blog.csdn.net/Crazy9599/article/details/89842280
        //  所以当前先关闭再重新启用摄像头来规避该问题
        if (binding.swCamera.isChecked()) {
            ZGConfigHelper.sharedInstance().enableCamera(false);
            ZGConfigHelper.sharedInstance().enableCamera(true);
        }
        if (binding.swMic.isChecked()) {
            ZGConfigHelper.sharedInstance().enableMic(false);
            ZGConfigHelper.sharedInstance().enableMic(true);
        }
        super.onResume();
    }

    public void goSetting(View view) {
        PublishSettingActivityUI.actionStart(this);
    }

    @Override
    protected void onDestroy() {
        // 停止所有的推流和拉流后，才能执行 logoutRoom
        ZGPublishHelper.sharedInstance().stopPreviewView();
        ZGPublishHelper.sharedInstance().stopPublishing();

        // 当用户退出界面时退出登录房间
        ZGBaseHelper.sharedInstance().loginOutRoom();
        super.onDestroy();
    }

    /**
     * button 点击事件
     * 开始推流
     */
    public void onStart(View view) {
        streamID = layoutBinding.edStreamId.getText().toString();
        // 隐藏输入StreamID布局
        hideInputStreamIDLayout();

        streamQuality.setStreamID(String.format("StreamID : %s", streamID));

        // 开始推流
        boolean isPublishSuccess = ZGPublishHelper.sharedInstance().startPublishing(streamID, "", ZegoConstants.PublishFlag.JoinPublish);

        if (!isPublishSuccess) {
            AppLogger.getInstance().i(ZGPublishHelper.class, "推流失败, streamID : %s", streamID);
            Toast.makeText(PublishActivityUI.this, getString(com.zego.common.R.string.tx_publish_fail), Toast.LENGTH_SHORT).show();
            // 修改标题状态拉流失败状态
            binding.title.setTitleName(getString(com.zego.common.R.string.tx_publish_fail));
        }

    }

    private void hideInputStreamIDLayout() {
        // 隐藏InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.GONE);
        binding.publishStateView.setVisibility(View.VISIBLE);
        hideKeyboard(ZegoApplication.zegoApplication, layoutBinding.edStreamId);
    }

    private void showInputStreamIDLayout() {
        // 显示InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.VISIBLE);
        binding.publishStateView.setVisibility(View.GONE);
    }

    //隐藏虚拟键盘
    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);

        }
    }


    public static void actionStart(Activity activity, String roomID) {
        Intent intent = new Intent(activity, PublishActivityUI.class);
        intent.putExtra("roomID", roomID);
        activity.startActivity(intent);
    }

    public void goCodeDemo(View view) {
        WebActivity.actionStart(this, "https://doc.zego.im/CN/209.html", getString(R.string.tx_publish_guide));
    }
}
