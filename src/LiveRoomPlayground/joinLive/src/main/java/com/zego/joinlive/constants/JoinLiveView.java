package com.zego.joinlive.constants;

import android.view.TextureView;
import android.widget.Button;

import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;

public class JoinLiveView {

    // 是否是推流视图
    public boolean isPublishView;

    // 播放的流名
    public String streamID;
    // 渲染视频的视图
    public TextureView textureView;

    private ZegoLiveRoom mZegoLiveRoom = null;

    public JoinLiveView(TextureView textureView, boolean isPublishView, String streamID){
        this.isPublishView = isPublishView;
        this.textureView = textureView;
        this.streamID = streamID;
    }

    public void setZegoLiveRoom(ZegoLiveRoom zegoLiveRoom){
        mZegoLiveRoom = zegoLiveRoom;
    }

    // 视图是否可用
    public boolean isFree(){
        return streamID.equals("");
    }

    public void setFree(){
        streamID = "";
        isPublishView = false;
    }

    /**
     * 交换view，跟大的view进行交换
     * @param bigView 大view
     */
    public void exchangeView(JoinLiveView bigView){
        // 大view交换到小view上去
        if (bigView.isPublishView){
            if (mZegoLiveRoom != null){
                mZegoLiveRoom.setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                mZegoLiveRoom.setPreviewView(this.textureView);
            }
        } else {
            if (mZegoLiveRoom != null){
                mZegoLiveRoom.updatePlayView(bigView.streamID, textureView);
            }
        }

        // 小view交换到大view上去
        if (this.isPublishView){
            if (mZegoLiveRoom != null) {
                mZegoLiveRoom.setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                mZegoLiveRoom.setPreviewView(bigView.textureView);
            }
        } else {
            if (mZegoLiveRoom != null){
                mZegoLiveRoom.updatePlayView(this.streamID, bigView.textureView);
            }
        }

        // 交换流信息
        String tmpStreamID = this.streamID;
        this.streamID = bigView.streamID;
        bigView.streamID = tmpStreamID;

        boolean tmpIsPublish = this.isPublishView;
        this.isPublishView = bigView.isPublishView;
        bigView.isPublishView = tmpIsPublish;

    }
}
