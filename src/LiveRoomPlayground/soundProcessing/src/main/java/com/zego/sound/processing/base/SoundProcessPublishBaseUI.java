package com.zego.sound.processing.base;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGConfigHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.entity.StreamQuality;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.common.widgets.CustomDialog;
import com.zego.sound.processing.R;
import com.zego.sound.processing.databinding.ActivitySoundProcessPublishBinding;
import com.zego.sound.processing.databinding.InputRoomIdLayoutBinding;
import com.zego.sound.processing.ui.SoundProcessPublishUI;
import com.zego.sound.processing.view.SoundEffectDialog;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoResponseCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;

/**
 * Created by zego on 2019/5/8.
 *
 * 推流相关逻辑都在这里。主要目的是为了让开发者更方便阅读变声/混响/立体声的代码。
 *
 */

public class SoundProcessPublishBaseUI extends BaseActivity {


    protected ActivitySoundProcessPublishBinding binding;
    protected InputRoomIdLayoutBinding layoutBinding;
    protected StreamQuality streamQuality = new StreamQuality();

    // 音效控制 Dialog
    protected SoundEffectDialog soundEffectDialog;

    protected String streamID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sound_process_publish);

        layoutBinding = binding.layout;
        // 利用DataBinding 可以通过bean类驱动UI变化。
        // 方便快捷避免需要写一大堆 setText 等一大堆臃肿的代码。
        binding.setQuality(streamQuality);

        // 初始化 SDK 回调代理
        initSDKCallback();

        // 调用sdk 开始预览接口 设置view 启用预览
        ZGPublishHelper.sharedInstance().startPreview(binding.publishView);
    }

    /**
     * 懒加载TipDialog
     *
     * @return 返回页面公用的TipDialog
     */
    protected SoundEffectDialog getSoundEffectDialog() {
        if (soundEffectDialog == null) {
            soundEffectDialog = new SoundEffectDialog(this);
        }
        return soundEffectDialog;
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
                    Toast.makeText(SoundProcessPublishBaseUI.this, getString(R.string.tx_publish_success), Toast.LENGTH_SHORT).show();
                } else {
                    binding.title.setTitleName(getString(R.string.tx_publish_fail));
                    AppLogger.getInstance().i(ZGPublishHelper.class, "推流失败, streamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(SoundProcessPublishBaseUI.this, getString(R.string.tx_publish_fail), Toast.LENGTH_SHORT).show();
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
                streamQuality.setResolution(String.format("分辨率: %dX%d", zegoPublishStreamQuality.width, zegoPublishStreamQuality.height));
            }

            @Override
            public void onCaptureVideoSizeChangedTo(int width, int height) {

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
    }



    /**
     * Button点击事件
     * 确认推流
     *
     * @param view
     */
    public void onConfirmPublish(View view) {
        final String roomId = layoutBinding.edRoomId.getText().toString();
        if (!"".equals(roomId)) {
            CustomDialog.createDialog("登录房间中...", this).show();
            // 开始推流前需要先登录房间
            ZGBaseHelper.sharedInstance().loginRoom(roomId, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
                @Override
                public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                    CustomDialog.createDialog(SoundProcessPublishBaseUI.this).cancel();
                    if (errorCode == 0) {
                        AppLogger.getInstance().i(SoundProcessPublishUI.class, "登陆房间成功 roomId : %s", roomId);

                        // 登陆房间成功，开始推流
                        startPublish(roomId);
                    } else {
                        AppLogger.getInstance().i(SoundProcessPublishUI.class, "登陆房间失败, errorCode : %d", errorCode);
                        Toast.makeText(SoundProcessPublishBaseUI.this, getString(com.zego.common.R.string.tx_login_room_failure), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(SoundProcessPublishBaseUI.this, getString(com.zego.common.R.string.tx_room_id_is_no_null), Toast.LENGTH_SHORT).show();
            AppLogger.getInstance().i(SoundProcessPublishUI.class, getString(com.zego.common.R.string.tx_room_id_is_no_null));
        }
    }

    // 开始推流
    protected void startPublish(String roomId) {
        streamID = ZegoUtil.getPublishStreamID();
        // 隐藏输入RoomID布局
        hideInputRoomIDLayout();

        // 更新界面RoomID 与 StreamID 信息
        streamQuality.setRoomID(String.format("roomID: %s", roomId));
        streamQuality.setStreamID(String.format("streamID: %s", streamID));

        // 开始推流 推流使用 JoinPublish 连麦模式，可降低延迟
        ZGPublishHelper.sharedInstance().startPublishing(streamID, "", ZegoConstants.PublishFlag.JoinPublish);

    }

    protected void hideInputRoomIDLayout() {
        // 隐藏InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.GONE);
        binding.publishStateView.setVisibility(View.VISIBLE);
    }

    //  某些华为手机上，应用在后台超过2分钟左右，华为系统会把摄像头资源给释放掉，并且可能会断开你应用的网络连接
    //  关于后台会断开网络的问题可以通过在设置-应用-权限管理-菜单-特殊访问权限-电池优化，将设置成不允许使用电池优化，才能解决。
    @Override
    protected void onResume() {
        super.onResume();
        // 华为某些机器会在应用退后台，或者锁屏的时候释放掉摄像头。达到省电的目的。
        // 所以这里做了一个关开摄像头的操作，以便恢复摄像头。但是这种做法是不推荐的。
        // 推荐使用interruptHandle 打断事件处理专题 中的方式去处理
        ZGConfigHelper.sharedInstance().enableCamera(false);
        ZGConfigHelper.sharedInstance().enableCamera(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        getSoundEffectDialog().release();

        // 停止所有的推流和拉流后，才能执行 logoutRoom
        ZGPublishHelper.sharedInstance().stopPreviewView();
        ZGPublishHelper.sharedInstance().stopPublishing();

        // 当退出界面时退出登陆房间
        ZGBaseHelper.sharedInstance().loginOutRoom();
    }
}
