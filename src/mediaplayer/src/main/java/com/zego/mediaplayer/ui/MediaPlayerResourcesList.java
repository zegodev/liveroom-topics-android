package com.zego.mediaplayer.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.zego.mediaplayer.ZGMediaPlayerDemoHelper;
import com.zego.mediaplayer.entity.ZGResourcesInfo;

import zego.com.mediaplayer.R;
import zego.com.mediaplayer.databinding.ActivityMediaResourcesListBinding;

/**
 * Created by zego on 2018/10/18.
 */

public class MediaPlayerResourcesList extends AppCompatActivity {


    private ActivityMediaResourcesListBinding binding;
    private String[] items;
    private ZGMediaPlayerDemoHelper zgMediaPlayerDemoHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_media_resources_list);
        setTitle("Topics");
        // 获取视频数据
        zgMediaPlayerDemoHelper = ZGMediaPlayerDemoHelper.sharedInstance();
        items = new String[zgMediaPlayerDemoHelper.getMediaList().size()];
        for (int i = 0; i < zgMediaPlayerDemoHelper.getMediaList().size(); i++) {
            ZGResourcesInfo zgResourcesInfo = zgMediaPlayerDemoHelper.getMediaList().get(i);
            items[i] = zgResourcesInfo.getMediaNameKey();
        }

        ArrayAdapter adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, items);
        binding.resourcesList.setAdapter(adapter);
        binding.resourcesList.setOnItemClickListener((parent, view, position, id) -> {
            ZGResourcesInfo zgResourcesInfo = ZGMediaPlayerDemoHelper.sharedInstance().getMediaList().get(position);
            Intent intent = new Intent(MediaPlayerResourcesList.this, MediaPlayerDemoUI.class);
            intent.putExtra("value", zgResourcesInfo);
            MediaPlayerResourcesList.this.startActivity(intent);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
