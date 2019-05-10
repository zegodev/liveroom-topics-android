package com.zego.common.entity;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.zego.common.BR;

/**
 * Created by zego on 2019/3/20.
 */

public class StreamQuality extends BaseObservable {
    private String roomID = "";
    private String streamID = "";
    private String resolution = "";
    private String bitrate = "";
    private String fps = "";

    @Bindable
    public String getRoomID() {
        return roomID;
    }

    public void setRoomID(String roomID) {
        this.roomID = roomID;
        notifyPropertyChanged(BR.roomID);
    }

    @Bindable
    public String getStreamID() {
        return streamID;
    }

    public void setStreamID(String streamID) {
        this.streamID = streamID;
        notifyPropertyChanged(BR.streamID);
    }

    @Bindable
    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
        notifyPropertyChanged(BR.resolution);
    }

    @Bindable
    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
        notifyPropertyChanged(BR.bitrate);
    }

    @Bindable
    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
        notifyPropertyChanged(BR.fps);
    }
}
