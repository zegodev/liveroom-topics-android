package com.zego.l3play;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.common.widgets.CustomDialog;
import com.zego.common.widgets.CustomPopWindow;
import com.zego.l3play.databinding.ActivityL3PlayInitSdkBinding;
import com.zego.zegoavkit2.ZegoStreamExtraPlayInfo;
import com.zego.zegoavkit2.mediaside.IZegoMediaSideCallback;
import com.zego.zegoavkit2.mediaside.ZegoMediaSideInfo;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.nio.ByteBuffer;
import java.util.Random;

public class L3PlayActivityUI extends BaseActivity implements View.OnClickListener {
    ActivityL3PlayInitSdkBinding binding;

    String pubStreamID = "";
    String playStreamID = "";
    String roomID = "";

    ZegoMediaSideInfo zegoMediaSideInfo = new ZegoMediaSideInfo();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 页面退出时释放sdk (这里开发者无需参考，这是根据自己业务需求来决定什么时候释放)
        ZGBaseHelper.sharedInstance().unInitZegoSDK();
    }

    @Override
    protected void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        roomID = "99999";;
        pubStreamID = Integer.toString(new Random().nextInt()).substring(1,7);
        playStreamID = pubStreamID;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_l3_play_init_sdk);

        ZGBaseHelper.sharedInstance().initZegoSDK(ZegoUtil.getAppID(), ZegoUtil.getAppSign(), false, new IZegoInitSDKCompletionCallback() {
            @Override
            public void onInitSDK(int errorCode) {

                // errorCode 非0 代表初始化sdk失败
                // 具体错误码说明请查看<a> https://doc.zego.im/CN/308.html </a>
                if (errorCode == 0) {
                    AppLogger.getInstance().i(L3PlayActivityUI.class, "初始化zegoSDK成功");
                    ZegoLiveRoom liveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
                    liveRoom.setPreviewView(binding.previewView);
                    liveRoom.startPreview();
                    Toast.makeText(L3PlayActivityUI.this, getString(com.zego.common.R.string.tx_init_success), Toast.LENGTH_SHORT).show();
                } else {
                    AppLogger.getInstance().i(L3PlayActivityUI.class, "初始化sdk失败 错误码 : %d", errorCode);
                    Toast.makeText(L3PlayActivityUI.this, getString(com.zego.common.R.string.tx_init_failure), Toast.LENGTH_SHORT).show();
                }
            }
        });

        zegoMediaSideInfo.setZegoMediaSideCallback(new IZegoMediaSideCallback() {
            @Override
            public void onRecvMediaSideInfo(String streamID, ByteBuffer inData, int dataLen) {
                int mediaType = getIntFrom(inData, dataLen);

                if (1001 == mediaType){
                    //SDK packet
                    String mediaSideInfoStr = getStringFrom(inData, dataLen);
                    long time = System.currentTimeMillis();
                    binding.timeRemote.setText(Long.toString(time));
                }
            }
        });
    }

    public void onLoginButtonTapped(View view) {
        long time = System.currentTimeMillis();
        String userID = Long.toString(time);
        ZegoLiveRoom.setUser(userID, userID);
        loginRoom();
    }

    public void onPublishButtonTapped(View view) {
        ZegoLiveRoom liveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
        liveRoom.startPublishing(pubStreamID, "123title", 0);
    }

    public void onPublishDateButtonTapped(View view) {
        long time = System.currentTimeMillis();
        sendSEI(Long.toString(time));
        binding.timeLocal.setText(Long.toString(time));
    }




    private void sendSEI(String content) {
        if (content.length() > 0) {
            if (content.getBytes().length > 1000) {
                return;
            }
            ByteBuffer inData = ByteBuffer.allocateDirect(content.getBytes().length);
            inData.put(content.getBytes(), 0,content.getBytes().length);
            inData.flip();


            zegoMediaSideInfo.sendMediaSideInfo(inData, content.getBytes().length, false, 0);
            AppLogger.getInstance().i(L3PlayActivityUI.class, content);
        }

    }

    public void onPlayButtonTapped(View view) {
        ZegoLiveRoom liveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
        liveRoom.stopPlayingStream(playStreamID);
        liveRoom.startPlayingStream(playStreamID, binding.playView);
    }

    public void onL3PlayButtonTapped(View view) {
        ZegoLiveRoom liveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
        ZegoStreamExtraPlayInfo extraPlayInfo = new ZegoStreamExtraPlayInfo();
        extraPlayInfo.mode = ZegoStreamExtraPlayInfo.ZegoStreamResourceMode.L3Only;
        liveRoom.stopPlayingStream(playStreamID);
        liveRoom.startPlayingStream(playStreamID, binding.playView, extraPlayInfo);
    }
    public void onResetButtonTapped(View view) {
        ZegoLiveRoom liveRoom = ZGBaseHelper.sharedInstance().getZegoLiveRoom();
        liveRoom.stopPlayingStream(playStreamID);
        liveRoom.stopPublishing();
    }



    /**
     * Button 点击事件触发
     * <p>
     * 进行Zego SDK的初始化。
     */
    public void onInitSDK(View view) {
        AppLogger.getInstance().i(L3PlayActivityUI.class, "点击 初始化SDK按钮");
//        boolean testEnv = binding.testEnv.isChecked();
        // 防止用户点击，弹出加载对话框
        // 调用sdk接口, 初始化sdk
        boolean results = ZGBaseHelper.sharedInstance().initZegoSDK(ZegoUtil.getAppID(), ZegoUtil.getAppSign(), ZegoUtil.getIsTestEnv(), new IZegoInitSDKCompletionCallback() {
            @Override
            public void onInitSDK(int errorCode) {

                // errorCode 非0 代表初始化sdk失败
                // 具体错误码说明请查看<a> https://doc.zego.im/CN/308.html </a>
                if (errorCode == 0) {
                    AppLogger.getInstance().i(L3PlayActivityUI.class, "初始化zegoSDK成功");
                    Toast.makeText(L3PlayActivityUI.this, getString(com.zego.common.R.string.tx_init_success), Toast.LENGTH_SHORT).show();
                } else {
                    AppLogger.getInstance().i(L3PlayActivityUI.class, "初始化sdk失败 错误码 : %d", errorCode);
                    Toast.makeText(L3PlayActivityUI.this, getString(com.zego.common.R.string.tx_init_failure), Toast.LENGTH_SHORT).show();
                }
            }
        });


        // 如果接口调用失败，也需要关闭对话框
        if (!results) {
            // 关闭加载对话框
            CustomDialog.createDialog(L3PlayActivityUI.this).cancel();
        }


    }

    /**
     * 跳转登陆页面
     */
    private void loginRoom() {
        // 登陆房间
        ZGBaseHelper.sharedInstance().loginRoom(roomID, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                if (errorCode == 0) {
                    Toast.makeText(L3PlayActivityUI.this, L3PlayActivityUI.this.getString(com.zego.common.R.string.tx_login_room_success), Toast.LENGTH_SHORT).show();
                    zegoMediaSideInfo.setMediaSideFlags(true, false,0);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {

    }


    /**
     * 显示描述窗口
     *
     * @param msg  显示内容
     * @param view
     */
    private void showPopWindows(String msg, View view) {
        //创建并显示popWindow
        new CustomPopWindow.PopupWindowBuilder(this)
                .enableBackgroundDark(true) //弹出popWindow时，背景是否变暗
                .setBgDarkAlpha(0.7f) // 控制亮度
                .create()
                .setMsg(msg)
                .showAsDropDown(view, 0, 20);
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, L3PlayActivityUI.class);
        activity.startActivity(intent);

    }



    // 获取mediaType类型值
    public int getIntFrom(ByteBuffer byteBuffer, int dataLen) {

        if (dataLen == 0) {
            return -1;
        }

        int result = (byteBuffer.get(0) & 0xFF) << 24 | (byteBuffer.get(1) & 0xFF) << 16 | (byteBuffer.get(2) & 0xFF) << 8 | (byteBuffer.get(3) & 0xFF);

        return result;
    }

    public String getStringFrom(ByteBuffer byteBuffer, int dataLen) {
        if (dataLen == 0) {
            return "";
        }

        byte[] temp = new byte[dataLen - 4];
        for (int i = 0; i < dataLen - 4; i++) {
            temp[i] = byteBuffer.get(i + 4);
        }

        return new String(temp);
    }
}

