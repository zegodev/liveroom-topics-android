package com.zego.layeredcoding;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.zego.common.GetAppIdConfig;
import com.zego.common.ZGManager;
import com.zego.common.util.DeviceInfoManager;
import com.zego.common.util.ZegoUtil;
import com.zego.layeredcoding.entity.RoomInfo;
import com.zego.layeredcoding.entity.RoomInfoEx;
import com.zego.zegoliveroom.constants.ZegoConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ZGLayeredCodingDemoHelper {

    static private ZGLayeredCodingDemoHelper zgLayeredCodingDemoHelper;

    private ArrayList<HashMap<String,Object>>  mRoomList = null;

    private UpdateRoomListCallback updateRoomListCallback = null;

    public static ZGLayeredCodingDemoHelper sharedInstance() {
        synchronized (ZGLayeredCodingDemoHelper.class) {
            if (zgLayeredCodingDemoHelper == null) {
                zgLayeredCodingDemoHelper = new ZGLayeredCodingDemoHelper();
            }
        }
        return zgLayeredCodingDemoHelper;
    }

    public String generateRoomID(Context context) {
        String roomID = "zglc-"+System.currentTimeMillis()+"-"+ DeviceInfoManager.generateDeviceId(context);

        return roomID;
    }

    public int getVideoLayer(String layerChoice) {
        int videoLyaer = ZegoConstants.VideoStreamLayer.VideoStreamLayer_Auto;
        switch (layerChoice) {
            case "BaseLayer":
                videoLyaer = ZegoConstants.VideoStreamLayer.VideoStreamLayer_BaseLayer;
                break;
            case "ExtendLayer":
                videoLyaer = ZegoConstants.VideoStreamLayer.VideoStreamLayer_ExtendLayer;
                break;
            default:
                break;
        }
        return videoLyaer;
    }

    public void requestRoomList(Context context) {

        boolean isTestEnv = ZGManager.sharedInstance().isTestEnv();
        String url = "";
        if (isTestEnv) {
            //测试环境请求地址
            url = "https://test2-liveroom-api.zego.im/demo/roomlist?appid=" + String.valueOf(ZegoUtil.getAppID());

        } else {
            //正式环境请求地址
            url = String.format("https://liveroom%d-api.zego.im/demo/roomlist?appid=%s", ZegoUtil.getAppID(), ZegoUtil.getAppID());
        }
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Gson gson = new Gson();
                RoomInfoEx roomInfoEx = gson.fromJson(response, RoomInfoEx.class);

                if (roomInfoEx != null && roomInfoEx.data != null) {

                    List<RoomInfo> pickedRoomInfos = new ArrayList();
                    List<RoomInfo> roomInfos = roomInfoEx.data.room_list;

                    for (int i = 0; i < roomInfos.size(); i++) {

                        if ((roomInfos.get(i).room_id.length() > 4) && (roomInfos.get(i).room_id.substring(0, 4).equals("zglc"))) {
                            pickedRoomInfos.add(roomInfos.get(i));
                        }
                    }

                    if (null != updateRoomListCallback){
                        updateRoomListCallback.onUpdateRoomListCallback(pickedRoomInfos);
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                if (null != updateRoomListCallback){
                    updateRoomListCallback.onRequestRoomListError(error.getMessage());
                }
            }
        });

        request.setRetryPolicy(new DefaultRetryPolicy(3000,0,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    public void setUpdateRoomListCallback(UpdateRoomListCallback callback) {
        updateRoomListCallback = callback;
    }

    public void unSetUpdateRoomListCallback() {
        updateRoomListCallback = null;
    }

    public interface UpdateRoomListCallback {
        void onUpdateRoomListCallback(List<RoomInfo> roomInfos);

        void onRequestRoomListError(String error);
    }
}
