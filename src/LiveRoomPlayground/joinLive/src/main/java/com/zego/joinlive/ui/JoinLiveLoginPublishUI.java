package com.zego.joinlive.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.common.widgets.CustomDialog;
import com.zego.joinlive.R;
import com.zego.joinlive.databinding.ActivityJoinLiveLoginPublishBinding;
import com.zego.zegoavkit2.audioprocessing.ZegoAudioReverbMode;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

public class JoinLiveLoginPublishUI extends BaseActivity {

    private ActivityJoinLiveLoginPublishBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_join_live_login_publish);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    // 登录房间
    public void onClickLoginRoomAndPublish(View view){

        String roomID = binding.edRoomId.getText().toString();
        if (!"".equals(roomID)) {
            jumpPublish(roomID);
        } else {
            Toast.makeText(JoinLiveLoginPublishUI.this, getString(com.zego.common.R.string.tx_room_id_is_no_null), Toast.LENGTH_SHORT).show();
            AppLogger.getInstance().i(JoinLiveLoginPublishUI.class, getString(com.zego.common.R.string.tx_room_id_is_no_null));
        }
    }


    // 跳转到主播推流页面
    public void jumpPublish(String roomID){
        JoinLiveAnchorUI.actionStart(JoinLiveLoginPublishUI.this, roomID);
    }

    /**
     * 供其他Activity调用，进入本专题的方法
     *
     * @param activity
     */
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, JoinLiveLoginPublishUI.class);
        activity.startActivity(intent);
    }
}
