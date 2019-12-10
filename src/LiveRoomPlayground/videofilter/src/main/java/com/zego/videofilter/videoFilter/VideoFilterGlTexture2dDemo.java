package com.zego.videofilter.videoFilter;

import android.graphics.SurfaceTexture;

import com.zego.videofilter.faceunity.FURenderer;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilter;

import java.nio.ByteBuffer;

/**
 * 外部滤镜采用 BUFFER_TYPE_SYNC_GL_TEXTURE_2D（同步传递 texture2d）方式传递数据给 SDK。
 *
 * SDK 不推荐采用同步滤镜（BUFFER_TYPE_SYNC_GL_TEXTURE_2D）实现外部滤镜，
 * 因为在同一线程中，OpenGL ES 的上下文、设置、uniform、attribute 是共用的，
 * 倘若对 OpenGL ES 不是很熟悉，极易在细节上出现不可预知的 Bug。
 *
 * Created by robotding on 17/2/23.
 */

public class VideoFilterGlTexture2dDemo extends ZegoVideoFilter {

    // SDK 内部实现 ZegoVideoFilter.Client 协议的对象
    private ZegoVideoFilter.Client mClient = null;

    // faceunity 美颜处理类
    private FURenderer mFURenderer;

    private boolean needDropFrame = true;

    public VideoFilterGlTexture2dDemo(FURenderer fuRenderer){
        this.mFURenderer = fuRenderer;
    }

    /**
     * 初始化资源，比如图像绘制、美颜组件等
     * @param client SDK 内部实现 ZegoVideoFilter.Client 协议的对象
     *
     * 注意：client 必须保存为强引用对象，在 stopAndDeAllocate 被调用前必须一直被保存。
     *      SDK 不负责管理 client 的生命周期。
     */
    @Override
    protected void allocateAndStart(Client client) {
        needDropFrame = true;

        mClient = client;

        // 创建及初始化 faceunity 相应的资源
        mFURenderer.onSurfaceCreated();
    }

    /**
     * 释放资源
     * 注意：必须调用 client 的 destroy 方法，否则会造成内存泄漏。
     */
    @Override
    protected void stopAndDeAllocate() {
        // 销毁 faceunity 相关的资源
        mFURenderer.onSurfaceDestroyed();

        // 建议在同步停止滤镜任务后再清理 client 对象，保证 SDK 调用 stopAndDeAllocate 后，没有残留的异步任务导致野指针 crash
        mClient.destroy();
        mClient = null;
    }

    /**
     * 指定滤镜的传递数据类型，同步传递 texture2d
     * SDK 需要根据 supportBufferType 返回的类型值创建不同的 client 对象。
     * @return
     */
    @Override
    protected int supportBufferType() {
        return BUFFER_TYPE_SYNC_GL_TEXTURE_2D;
    }

    @Override
    protected int dequeueInputBuffer(int width, int height, int stride) {
        return 0;
    }

    @Override
    protected ByteBuffer getInputBuffer(int index) {
        return null;
    }

    @Override
    protected void queueInputBuffer(int bufferIndex, int width, int height, int stride, long timestamp_100n) {

    }

    @Override
    protected SurfaceTexture getSurfaceTexture() {
        return null;
    }

    /**
     * 绘制图像
     * @param zegoTextureId SDK 采集数据的纹理 ID
     * @param width 图像数据宽
     * @param height 图像数据高
     * @param timestamp_100n 时间戳
     */
    @Override
    protected void onProcessCallback(int zegoTextureId, int width, int height, long timestamp_100n) {

        // 首帧需要直接使用 ZEGO 显示，但需要将纹理传给FU，保证FU将脏数据处理掉
        if (needDropFrame) {
            mClient.onProcessCallback(zegoTextureId, width, height, timestamp_100n);
            mFURenderer.onDrawFrame(zegoTextureId, width, height);
            needDropFrame = false;
        } else {  // 后续的就可以按照正常的方式进行处理
            // 传入 SDK 抛出的采集数据的纹理 ID 使用 faceunity 进行美颜，返回美颜后数据的纹理 ID
            int textureId = mFURenderer.onDrawFrame(zegoTextureId, width, height);

            // 使用此 textureId 用做本地预览的视图渲染
            mClient.onProcessCallback(textureId, width, height, timestamp_100n);
        }
    }
}
