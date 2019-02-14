package com.zego.videocapture.videocapture;

import android.content.Context;


import com.zego.zegoavkit2.ZegoVideoCaptureDevice;
import com.zego.zegoavkit2.ZegoVideoCaptureFactory;

/**
 * Created by robotding on 16/6/5.
 */
public class VideoCaptureFactoryDemo extends ZegoVideoCaptureFactory {
    private CaptureOrigin origin = CaptureOrigin.CaptureOrigin_Image;
    private ZegoVideoCaptureDevice mDevice = null;
    private Context mContext = null;

    private Boolean isCapture = false;

    public enum CaptureOrigin{
        CaptureOrigin_Image,
        CaptureOrigin_ImageV2,
        CaptureOrigin_Screen,
        CaptureOrigin_Camera,
        CaptureOrigin_CameraV2
    }

    public VideoCaptureFactoryDemo(CaptureOrigin origin){
        this.origin = origin;
    }

    public void setIsCapture(Boolean isCapture){
        this.isCapture = isCapture;
    }

    public ZegoVideoCaptureDevice create(String device_id) {
        if (origin == CaptureOrigin.CaptureOrigin_Camera) {
            mDevice = new VideoCaptureFromCamera();
        } else if (origin == CaptureOrigin.CaptureOrigin_Image) {
            mDevice = new VideoCaptureFromImage(mContext);
        } else if (origin == CaptureOrigin.CaptureOrigin_ImageV2) {
            mDevice = new VideoCaptureFromImage2(mContext);
        } else if (origin == CaptureOrigin.CaptureOrigin_CameraV2) {
            mDevice = new VideoCaptureFromCamera2();
        }

        return mDevice;
    }

    public void destroy(ZegoVideoCaptureDevice vc) {
        mDevice = null;
    }

    public void setContext(Context context) {
        mContext = context;
    }

}
