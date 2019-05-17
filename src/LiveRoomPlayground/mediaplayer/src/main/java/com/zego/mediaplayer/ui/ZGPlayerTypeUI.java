package com.zego.mediaplayer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.zego.common.ui.BaseActivity;
import com.zego.mediaplayer.R;

public class ZGPlayerTypeUI extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_type);
    }

    public void jumSinglePlayer(View view) {
        Intent intent = new Intent(ZGPlayerTypeUI.this, MediaPlayerResourcesList.class);
        ZGPlayerTypeUI.this.startActivity(intent);
    }

    public void jumMultiPlayer(View view) {
        Intent intent = new Intent(ZGPlayerTypeUI.this, ZGMultiPlayerDemoUI.class);
        ZGPlayerTypeUI.this.startActivity(intent);
    }

}
