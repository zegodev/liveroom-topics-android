package com.zego.mixstream.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.zego.common.ui.BaseActivity;
import com.zego.mixstream.R;
import com.zego.mixstream.ZGMixStreamDemoHelper;
import com.zego.mixstream.entity.RoomInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ZGMixStreamRoomListUI extends BaseActivity implements ZGMixStreamDemoHelper.UpdateRoomListCallback{

    private ListView mRoomListView;
    private TextView mQueryStatusTxt;

    SimpleAdapter simpleAdapter;
    private List<RoomInfo> roomInfoList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("房间列表");
        setContentView(R.layout.activity_mix_roomlist);

        mRoomListView = (ListView)findViewById(R.id.roomlist);
        mQueryStatusTxt = (TextView)findViewById(R.id.querystatus_txt);

        ZGMixStreamDemoHelper.sharedInstance().setUpdateRoomListCallback(this);
        ZGMixStreamDemoHelper.sharedInstance().requestRoomList(this);

        mRoomListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                HashMap<String, String> map = (HashMap<String, String>)adapterView.getItemAtPosition(position);

                Intent intent = new Intent(ZGMixStreamRoomListUI.this, ZGMixAudienceUI.class);
                intent.putExtra("AnchorRoomID", map.get("RoomID"));
                intent.putExtra("AnchorRoomName", map.get("RoomName"));
                intent.putExtra("AnchorID", map.get("AnchorID"));
                ZGMixStreamRoomListUI.this.startActivity(intent);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mQueryStatusTxt.setText("");
        ZGMixStreamDemoHelper.sharedInstance().unSetUpdateRoomListCallback();
    }

    public void QueryRoomList(View view) {
        mQueryStatusTxt.setText("");
        ZGMixStreamDemoHelper.sharedInstance().requestRoomList(this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (roomInfoList.size() > 0) {
                    simpleAdapter = new SimpleAdapter(ZGMixStreamRoomListUI.this, ZGMixStreamRoomListUI.this.getItem(), R.layout.activity_roomitem,
                            new String[]{"RoomID", "RoomName", "AnchorID"}, new int[]{R.id.roomID, R.id.roomName, R.id.anchorID});
                    mRoomListView.setAdapter(simpleAdapter);
                }
            }
        });
    }


    public void CreateRoom(View view) {
        Intent intent = new Intent(ZGMixStreamRoomListUI.this, ZGMixAnchorUI.class);
        ZGMixStreamRoomListUI.this.startActivity(intent);
    }

    public ArrayList<HashMap<String, Object>> getItem() {
        ArrayList<HashMap<String, Object>> item = new ArrayList<HashMap<String, Object>>();
        if (roomInfoList != null) {
            for (int i = 0; i < roomInfoList.size(); i++) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                RoomInfo tmp = roomInfoList.get(i);

                map.put("RoomID", tmp.room_id);
                map.put("RoomName", tmp.room_name);
                map.put("AnchorID", tmp.anchor_id_name);
                item.add(map);
            }
        }
        return item;
    }

    @Override
    public void onUpdateRoomListCallback(List<RoomInfo> roomInfos) {
        roomInfoList = roomInfos;
        mQueryStatusTxt.setText("");
        if (roomInfoList.size() == 0) {
            mQueryStatusTxt.setText("room list is empty.");
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (roomInfoList.size() > 0) {
                    simpleAdapter = new SimpleAdapter(ZGMixStreamRoomListUI.this, ZGMixStreamRoomListUI.this.getItem(), R.layout.activity_roomitem,
                            new String[]{"RoomID", "RoomName", "AnchorID"}, new int[]{R.id.roomID, R.id.roomName, R.id.anchorID});
                    mRoomListView.setAdapter(simpleAdapter);
                }
            }
        });

    }

    @Override
    public void onRequestRoomListError(String error) {
        mQueryStatusTxt.setText("request room list fail, err: " + error);
    }
}
