package com.zego.videocommunication.model;

import android.util.Log;
import android.view.TextureView;
import android.widget.LinearLayout;

import com.zego.common.ZGPlayHelper;
import com.zego.videocommunication.ui.PublishStreamAndPlayStreamUI;
import com.zego.videocommunication.ui.VideoCommunicationMainUI;
import com.zego.videocommunicaton.databinding.PublishStreamAndPlayStreamBinding;

import java.util.ArrayList;
import java.util.LinkedHashMap;


/**
 * 本类为视频通话的推拉流UI布局Model类，由于与业务强相关，实现方式可不相同，这里仅作为示例参考用，业务应根据自己的需求自己实现
 *
 */
public class PublishStreamAndPlayStreamLayoutModel {

    private ArrayList<LinearLayout> arrayListLinearLayout = new ArrayList<>();

    private LinkedHashMap<LinearLayout, StreamidAndViewFlag> linearLayoutHasViewLinkedHashMap = new LinkedHashMap<>();

    PublishStreamAndPlayStreamUI mPublishStreamAndPlayStreamUI;


    /**
     * 定义一个单个渲染视图模型的内部类
     */
    class StreamidAndViewFlag{
        Boolean layoutHasViewFlag;
        String streamid;
        TextureView renderView;
        StreamidAndViewFlag(){
            this.layoutHasViewFlag = false;
            this.streamid = "";
            this.renderView = null;
        }
    }

    public PublishStreamAndPlayStreamLayoutModel(PublishStreamAndPlayStreamUI publishStreamAndPlayStreamUI){

        mPublishStreamAndPlayStreamUI = publishStreamAndPlayStreamUI;

        // 这里使用写死的方式创建12个推流或拉流的模型对象
        StreamidAndViewFlag streamidAndViewFlag0 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer0, streamidAndViewFlag0);
        StreamidAndViewFlag streamidAndViewFlag1 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer1, streamidAndViewFlag1);
        StreamidAndViewFlag streamidAndViewFlag2 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer2, streamidAndViewFlag2);
        StreamidAndViewFlag streamidAndViewFlag3 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer3, streamidAndViewFlag3);
        StreamidAndViewFlag streamidAndViewFlag4 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer4, streamidAndViewFlag4);
        StreamidAndViewFlag streamidAndViewFlag5 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer5, streamidAndViewFlag5);
        StreamidAndViewFlag streamidAndViewFlag6 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer6, streamidAndViewFlag6);
        StreamidAndViewFlag streamidAndViewFlag7 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer7, streamidAndViewFlag7);
        StreamidAndViewFlag streamidAndViewFlag8 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer8, streamidAndViewFlag8);
        StreamidAndViewFlag streamidAndViewFlag9 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer9, streamidAndViewFlag9);
        StreamidAndViewFlag streamidAndViewFlag10 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer10, streamidAndViewFlag10);
        StreamidAndViewFlag streamidAndViewFlag11 = new StreamidAndViewFlag();
        linearLayoutHasViewLinkedHashMap.put(publishStreamAndPlayStreamUI.getPublishStreamAndPlayStreamBinding().llViewContainer11, streamidAndViewFlag11);

    }

    /**
     * 当有新增推流时，调用此方法创建渲染的 TextureView，将其加入到布局中
     *
     * @param streamid
     * @return
     */
    public TextureView addStreamToViewInLayout(String streamid){

        TextureView renderView = new TextureView(this.mPublishStreamAndPlayStreamUI);

        for(LinearLayout linearLayout : this.linearLayoutHasViewLinkedHashMap.keySet()){

            if(this.linearLayoutHasViewLinkedHashMap.get(linearLayout).layoutHasViewFlag == false){

                this.linearLayoutHasViewLinkedHashMap.get(linearLayout).renderView = renderView;

                this.linearLayoutHasViewLinkedHashMap.get(linearLayout).layoutHasViewFlag = true;
                this.linearLayoutHasViewLinkedHashMap.get(linearLayout).streamid = streamid;
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                linearLayout.addView(renderView, layoutParams);
                break;

            }
        }

        return renderView;

    }

    /**
     * 当有流关闭时，调用此方法释放之前渲染的View
     * @param streamid
     */
    public void removeStreamToViewInLayout(String streamid){

        for(LinearLayout linearLayout : this.linearLayoutHasViewLinkedHashMap.keySet()){

            if(this.linearLayoutHasViewLinkedHashMap.get(linearLayout).streamid.equals(streamid)){
                linearLayout.removeView(this.linearLayoutHasViewLinkedHashMap.get(linearLayout).renderView);
                this.linearLayoutHasViewLinkedHashMap.get(linearLayout).renderView = null;
                this.linearLayoutHasViewLinkedHashMap.get(linearLayout).streamid = "";
                this.linearLayoutHasViewLinkedHashMap.get(linearLayout).layoutHasViewFlag = false;
                break;
            }

        }

    }

    /**
     * 当退出时，应释放所有渲染的View
     */
    public void removeAllStreamToViewInLayout(){

        for(LinearLayout linearLayout : this.linearLayoutHasViewLinkedHashMap.keySet()) {

            if (this.linearLayoutHasViewLinkedHashMap.get(linearLayout).layoutHasViewFlag == true) {
                linearLayout.removeView(this.linearLayoutHasViewLinkedHashMap.get(linearLayout).renderView);
                this.linearLayoutHasViewLinkedHashMap.get(linearLayout).renderView = null;
                this.linearLayoutHasViewLinkedHashMap.get(linearLayout).streamid = "";
                this.linearLayoutHasViewLinkedHashMap.get(linearLayout).layoutHasViewFlag = false;

            }
        }

    }
}
