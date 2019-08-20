package com.zego.common.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;

import com.zego.common.R;
import com.zego.common.R2;
import com.zego.common.adapter.LogListAdapter;
import com.zego.common.util.ShareUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by zego on 2018/4/2.
 */

public class ZegoLogShareActivity extends AbsBaseActivity {

    private LinkedList<String> mDatas;
    @Override
    protected int getContentViewLayout() {
        return R.layout.activity_log_share;
    }

    @BindView(R2.id.recyclerView)
    android.support.v7.widget.RecyclerView recyclerView;


    @Override
    protected void initExtraData(Bundle savedInstanceState) {
        String rootPath = com.zego.zegoavkit2.utils.ZegoLogUtil.getLogPath(ZegoLogShareActivity.this);
        File rootDir = new File(rootPath);
        String[] fileName = rootDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !TextUtils.isEmpty(name) && name.startsWith("zegoavlog") && name.endsWith(".txt");
            }
        });
        Log.e("test","*** logfile: " + fileName[0]);

       List<String> arrayList=Arrays.asList(fileName);
       mDatas = new LinkedList(arrayList);
    }

    @Override
    protected void initVariables(Bundle savedInstanceState) {

    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        LogListAdapter logListAdapter = new LogListAdapter(ZegoLogShareActivity.this);
        logListAdapter.setData(mDatas);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(logListAdapter);
        // 设置Item添加和移除的动画
        recyclerView.setItemAnimator(new DefaultItemAnimator());

    }

    @Override
    protected void loadData(Bundle savedInstanceState) {

    }

    @OnClick(R2.id.tv_back)
    public void back() {
        finish();
    }

    @OnClick(R2.id.share)
    public void share() {
        String rootPath = com.zego.zegoavkit2.utils.ZegoLogUtil.getLogPath(ZegoLogShareActivity.this);
        File rootDir = new File(rootPath);
        File[] logFiles = rootDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !TextUtils.isEmpty(name) && name.startsWith("zegoavlog") && name.endsWith(".txt");
            }
        });
        if (logFiles.length > 0) {
            ShareUtil.sendFiles(logFiles, ZegoLogShareActivity.this);
        } else {
            Log.w("ZegoLogShareActivity ", "not found any log files.");
        }

    }


    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, ZegoLogShareActivity.class);
        activity.startActivity(intent);
    }
}
