package com.zego.interrupthandler.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.entity.StreamQuality;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.common.widgets.CustomDialog;
import com.zego.interrupthandler.R;
import com.zego.interrupthandler.databinding.ActivityInterruptHandlerPublishBinding;
import com.zego.interrupthandler.databinding.InterruptHandlerInputRoomIdLayoutBinding;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoResponseCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;
import java.util.Locale;

/**
 * 推流页面
 */

public class InterruptHandlerPublishUI extends BaseActivity {


    private ActivityInterruptHandlerPublishBinding binding;
    private InterruptHandlerInputRoomIdLayoutBinding layoutBinding;
    private StreamQuality streamQuality = new StreamQuality();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_interrupt_handler_publish);

        layoutBinding = binding.layout;
        // 利用DataBinding 可以通过bean类驱动UI变化。
        // 方便快捷避免需要写一大堆 setText 等一大堆臃肿的代码。
        binding.setQuality(streamQuality);

        ZGPublishHelper.sharedInstance().startPreview(binding.publishView);

        // 初始化 SDK 回调代理
        initSDKCallback();
    }

    protected void initSDKCallback() {
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
                    Toast.makeText(InterruptHandlerPublishUI.this, getString(R.string.tx_publish_success), Toast.LENGTH_SHORT).show();
                } else {
                    binding.title.setTitleName(getString(R.string.tx_publish_fail));
                    AppLogger.getInstance().i(ZGPublishHelper.class, "推流失败, streamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(InterruptHandlerPublishUI.this, getString(R.string.tx_publish_fail), Toast.LENGTH_SHORT).show();
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
                streamQuality.setFps(String.format(Locale.CHINA, "帧率: %f", zegoPublishStreamQuality.vnetFps));
                streamQuality.setBitrate(String.format(Locale.CHINA, "码率: %f kbs", zegoPublishStreamQuality.vkbps));
                streamQuality.setResolution(String.format(Locale.CHINA, "分辨率: %dX%d", zegoPublishStreamQuality.width, zegoPublishStreamQuality.height));
            }

            @Override
            public AuxData onAuxCallback(int i) {
                // aux混音，可以将外部音乐混进推流中。类似于直播中添加伴奏，掌声等音效
                // 另外还能用于ktv场景中的伴奏播放
                // 想深入了解可以进入进阶功能中的-mixing。
                // <a>https://doc.zego.im/CN/253.html</a> 文档中有说明
                return null;
            }

            @Override
            public void onCaptureVideoSizeChangedTo(int width, int height) {

            }

            @Override
            public void onMixStreamConfigUpdate(int i, String s, HashMap<String, Object> hashMap) {
                // 混流配置更新时会回调该方法。
            }

            @Override
            public void onCaptureVideoFirstFrame() {
                // 当SDK采集摄像头捕获到第一帧时会回调该方法

            }
        });
    }


    /**
     * Button点击事件
     * 确认推流
     */
    public void onConfirmPublish(View view) {
        final String roomId = layoutBinding.edRoomId.getText().toString();
        if (!"".equals(roomId)) {
            CustomDialog.createDialog("登录房间中...", this).show();
            // 开始推流前需要先登录房间
            ZGBaseHelper.sharedInstance().loginRoom(roomId, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
                @Override
                public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                    CustomDialog.createDialog(InterruptHandlerPublishUI.this).cancel();
                    if (errorCode == 0) {
                        AppLogger.getInstance().i(InterruptHandlerPublishUI.class, "登陆房间成功 roomId : %s", roomId);

                        // 登陆房间成功，开始推流
                        startPublish(roomId);
                    } else {
                        AppLogger.getInstance().i(InterruptHandlerPublishUI.class, "登陆房间失败, errorCode : %d", errorCode);
                        Toast.makeText(InterruptHandlerPublishUI.this, getString(com.zego.common.R.string.tx_login_room_failure), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            ZGBaseHelper.sharedInstance().setZegoRoomCallback(new IZegoRoomCallback() {
                @Override
                public void onKickOut(int i, String s) {
                    binding.title.setTitleName(getString(R.string.disconnect_with_room));
                }

                @Override
                public void onDisconnect(int i, String s) {
                    binding.title.setTitleName(getString(R.string.disconnect_with_room));
                }

                @Override
                public void onReconnect(int i, String s) {

                }

                @Override
                public void onTempBroken(int i, String s) {

                }

                @Override
                public void onStreamUpdated(int i, ZegoStreamInfo[] zegoStreamInfos, String s) {

                }

                @Override
                public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String s) {

                }

                @Override
                public void onRecvCustomCommand(String s, String s1, String s2, String s3) {

                }
            });
        } else {
            Toast.makeText(InterruptHandlerPublishUI.this, getString(com.zego.common.R.string.tx_room_id_is_no_null), Toast.LENGTH_SHORT).show();
            AppLogger.getInstance().i(InterruptHandlerPublishUI.class, getString(com.zego.common.R.string.tx_room_id_is_no_null));
        }
    }

    // 开始推流
    public void startPublish(String roomId) {
        String streamID = ZegoUtil.getPublishStreamID();
        // 隐藏输入RoomID布局
        hideInputRoomIDLayout();

        // 更新界面RoomID 与 StreamID 信息
        streamQuality.setRoomID(String.format("roomID: %s", roomId));
        streamQuality.setStreamID(String.format("streamID: %s", streamID));

        // 开始推流 推流使用 JoinPublish 连麦模式，可降低延迟
        ZGPublishHelper.sharedInstance().startPublishing(streamID, "", ZegoConstants.PublishFlag.JoinPublish);

    }

    public void hideInputRoomIDLayout() {
        // 隐藏InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.GONE);
        binding.publishStateView.setVisibility(View.VISIBLE);
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, InterruptHandlerPublishUI.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 停止所有的推流和拉流后，才能执行 logoutRoom
        ZGPublishHelper.sharedInstance().stopPreviewView();
        ZGPublishHelper.sharedInstance().stopPublishing();

        // 当退出界面时退出登陆房间
        ZGBaseHelper.sharedInstance().loginOutRoom();
    }
}
