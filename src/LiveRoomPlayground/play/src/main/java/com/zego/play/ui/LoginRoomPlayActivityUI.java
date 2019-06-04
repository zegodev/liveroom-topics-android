package com.zego.play.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.zego.common.widgets.CustomDialog;
import com.zego.play.R;
import com.zego.play.databinding.ActivityLoginRoomBinding;
import com.zego.common.ZGBaseHelper;
import com.zego.common.constants.ZGLiveRoomConstants;
import com.zego.common.ui.BaseActivity;
import com.zego.common.ui.WebActivity;
import com.zego.common.util.AppLogger;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

/**
 * Created by zego on 2019/3/19.
 */

public class LoginRoomPlayActivityUI extends BaseActivity {

    ActivityLoginRoomBinding binding;

    private int roomRole = ZegoConstants.RoomRole.Audience;
    private String flag;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login_room);

        flag = getIntent().getStringExtra("flag");
        // 如果用户标记的动作是推流，则房间角色为 主播 否则为观众角色
        if ("publish".equals(flag)) {
            roomRole = ZegoConstants.RoomRole.Anchor;
        } else {
            roomRole = ZegoConstants.RoomRole.Audience;
        }

        // 恢复sdk默认设置.
        PlaySettingActivityUI.clearPlayConfig();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 页面退出时释放sdk (这里开发者无需参考，这是根据自己业务需求来决定什么时候释放)
        ZGBaseHelper.sharedInstance().unInitZegoSDK();
    }

    /**
     * button 点击事件
     * 登陆房间
     *
     * @param view
     */
    public void onLoginRoom(View view) {
        String roomId = binding.edRoomId.getText().toString();

        final String finalRoomId = roomId;
        // 防止用户点击，弹出加载对话框
        CustomDialog.createDialog("登陆房间中...", LoginRoomPlayActivityUI.this).show();

        // 登陆房间
        boolean isLoginRoomSuccess = ZGBaseHelper.sharedInstance().loginRoom(roomId, roomRole, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                // 关闭加载对话框
                CustomDialog.createDialog(LoginRoomPlayActivityUI.this).cancel();

                if (errorCode == 0) {
                    loginRoomCompletion(true, finalRoomId, errorCode);
                } else {
                    loginRoomCompletion(false, finalRoomId, errorCode);
                }
            }
        });

        if (!isLoginRoomSuccess) {
            loginRoomCompletion(false, finalRoomId, -1);
        }
    }


    /**
     * 处理登陆房间成功或失败的逻辑
     *
     * @param isLoginRoom 是否登陆房间成功
     * @param finalRoomId roomID
     * @param errorCode   登陆房间错误码
     */
    private void loginRoomCompletion(boolean isLoginRoom, String finalRoomId, int errorCode) {
        // 关闭加载对话框
        CustomDialog.createDialog(LoginRoomPlayActivityUI.this).cancel();
        if (isLoginRoom) {
            Toast.makeText(LoginRoomPlayActivityUI.this, getString(com.zego.common.R.string.tx_login_room_success), Toast.LENGTH_SHORT).show();
            AppLogger.getInstance().i(LoginRoomPlayActivityUI.class, "登陆房间成功 roomId : %s", finalRoomId);

            // 登陆房间成功，跳转推拉流页面
            jumpPlay(finalRoomId);
        } else {
            AppLogger.getInstance().i(LoginRoomPlayActivityUI.class, "登陆房间失败 errorCode : %s", errorCode == -1 ? "api调用失败" : String.valueOf(errorCode));
            Toast.makeText(LoginRoomPlayActivityUI.this, getString(com.zego.common.R.string.tx_login_room_failure), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 跳转拉流页面
     */
    private void jumpPlay(String roomID) {
        PlayActivityUI.actionStart(this, roomID);
    }

    /**
     * button 点击事件
     * <p>
     * 跳转到登陆房间指引webView
     *
     * @param view
     */
    public void goCodeDemo(View view) {
        WebActivity.actionStart(this, "https://doc.zego.im/CN/625.html", getString(com.zego.common.R.string.tx_login_room_guide));
    }

    public static void actionStart(Activity activity, String flag) {
        Intent intent = new Intent(activity, LoginRoomPlayActivityUI.class);
        // 标记我当前动作是推流还是拉流, 登陆房间后跳转页面需要用到
        intent.putExtra("flag", flag);
        activity.startActivity(intent);
    }
}
