package com.zego.videocapture.videocapture;

import android.content.Context;


import com.zego.zegoavkit2.ZegoVideoCaptureDevice;
import com.zego.zegoavkit2.ZegoVideoCaptureFactory;

/**
 * VideoCaptureFactoryDemo
 * 继承实现 ZEGO 外部采集工厂类，创建并保存外部采集设备实例
 * 注意：1.必须保证外部采集设备实例（ZegoVideoCaptureDevice）在create和destory之间是可用的
 *      2.ZegoVideoCaptureFactory 会缓存 ZegoVideoCaptureDevice 实例，开发者需避免创建新的实例，造成争抢独占设备
 */
public class VideoCaptureFactoryDemo extends ZegoVideoCaptureFactory {
    private CaptureOrigin origin = CaptureOrigin.CaptureOrigin_Image;
    private ZegoVideoCaptureDevice mDevice = null;
    private Context mContext = null;

    // 外部采集来源
    public enum CaptureOrigin{
        // 图片源，当前采集设备使用的数据传递类型是Surface_Texture
        CaptureOrigin_Image,
        // 图片源，当前采集设备使用的数据传递类型是GL_Texture_2D
        CaptureOrigin_ImageV2,
        // 屏幕源
        CaptureOrigin_Screen,
        // 摄像头源，当前采集设备使用的数据传递类型是YUV格式（内存拷贝）
        CaptureOrigin_Camera,
        // 摄像头源，当前采集设备使用的数据传递类型是Surface_Texture
        CaptureOrigin_CameraV2,
        // 摄像头源，当前采集设备使用的数据传递类型是ENCODED_FRAME（码流）
        CaptureOrigin_CameraV3
    }

    public VideoCaptureFactoryDemo(CaptureOrigin origin){
        this.origin = origin;
    }

    // 创建外部采集设备实例
    public ZegoVideoCaptureDevice create(String device_id) {
        if (origin == CaptureOrigin.CaptureOrigin_Camera) {
            mDevice = new VideoCaptureFromCamera();
        } else if (origin == CaptureOrigin.CaptureOrigin_Image) {
            mDevice = new VideoCaptureFromImage(mContext);
        } else if (origin == CaptureOrigin.CaptureOrigin_ImageV2) {
            mDevice = new VideoCaptureFromImage2(mContext);
        } else if (origin == CaptureOrigin.CaptureOrigin_CameraV2) {
            mDevice = new VideoCaptureFromCamera2();
        } else if (origin == CaptureOrigin.CaptureOrigin_CameraV3) {
            mDevice = new VideoCaptureFromCamera3();
        }

        return mDevice;
    }

    // 销毁外部采集设备实例
    public void destroy(ZegoVideoCaptureDevice vc) {
        mDevice = null;
    }

    public void setContext(Context context) {
        mContext = context;
    }

}
