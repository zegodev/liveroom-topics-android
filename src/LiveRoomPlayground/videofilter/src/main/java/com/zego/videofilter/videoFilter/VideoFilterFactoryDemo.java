package com.zego.videofilter.videoFilter;

import com.zego.videofilter.faceunity.FURenderer;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilter;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilterFactory;

/**
 * Created by robotding on 16/12/3.
 */

public class VideoFilterFactoryDemo extends ZegoVideoFilterFactory {
    private FilterType type = FilterType.FilterType_SurfaceTexture;
    private ZegoVideoFilter mFilter = null;

    // faceunity 美颜组件
    private FURenderer mFunRender;

    // 前处理传递数据的类型枚举
    public enum FilterType {
        FilterType_Mem,
        FilterType_SurfaceTexture,
        FilterType_HybridMem,
        FilterType_SyncTexture,
        FilterType_ASYNCI420Mem
    }

    public VideoFilterFactoryDemo(FilterType type, FURenderer fuRenderer) {
        this.type = type;
        this.mFunRender = fuRenderer;
    }

    // 创建外部滤镜实例
    public ZegoVideoFilter create() {
        switch (type) {
            case FilterType_Mem:
                mFilter = new VideoFilterMemDemo(mFunRender);
                break;
            case FilterType_SurfaceTexture:
                mFilter = new VideoFilterSurfaceTextureDemo(mFunRender);
                break;
            case FilterType_HybridMem:
                mFilter = new VideoFilterHybridDemo(mFunRender);
                break;
            case FilterType_SyncTexture:
                mFilter = new VideoFilterGlTexture2dDemo(mFunRender);
                break;
            case FilterType_ASYNCI420Mem:
                mFilter = new VideoFilterI420MemDemo(mFunRender);
                break;
        }

        return mFilter;
    }

    // 销毁外部滤镜实例
    public void destroy(ZegoVideoFilter vf) {
        mFilter = null;
    }
}
