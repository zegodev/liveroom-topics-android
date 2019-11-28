package com.zego.layeredcoding.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.zego.layeredcoding.R;

public class ZGCreateRoomUI extends AppCompatActivity {

    private ToggleButton mNetWorkToggle;
    private ToggleButton mCameraToggle;

    private EditText mRoomNameEdit;
    private Button mJoinBtn;

    private boolean useFrontCamera = true;
    private boolean useOptimisedNet = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("创建直播房间");
        setContentView(R.layout.activity_createroom);

        mNetWorkToggle = (ToggleButton)findViewById(R.id.tb_enable_net);
        mCameraToggle = (ToggleButton)findViewById(R.id.tb_enable_front_cam);
        mRoomNameEdit = (EditText)findViewById(R.id.roomName_edit);
        mJoinBtn = (Button)findViewById(R.id.join_btn);

        mCameraToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mCameraToggle.setChecked(true);
                    useFrontCamera = true;
                } else {
                    mCameraToggle.setChecked(false);
                    useFrontCamera = false;
                }
            }
        });

        mNetWorkToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mNetWorkToggle.setChecked(true);
                    useOptimisedNet = true;
                } else {
                    mNetWorkToggle.setChecked(false);
                    useOptimisedNet = false;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void JoinLiveRoom(View view) {

        String roomName = mRoomNameEdit.getText().toString();

        Intent intent = new Intent(ZGCreateRoomUI.this, ZGAnchorUI.class);
        intent.putExtra("RoomName", roomName);
        intent.putExtra("UseFrontCamera",useFrontCamera);
        intent.putExtra("UseOptimisedNet",useOptimisedNet);
        ZGCreateRoomUI.this.startActivity(intent);
    }
}
