package com.zego.playground.demo.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.zego.playground.demo.R;
import com.zego.playground.demo.entity.ModuleInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zego on 2018/2/6.
 */

public class MainAdapter extends RecyclerView.Adapter {

    private List<ModuleInfo> topicList = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.module_list, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final MyViewHolder myViewHolder = (MyViewHolder) holder;
        if (mOnItemClickListener != null) {
            myViewHolder.list.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    int position1 = myViewHolder.getLayoutPosition();
                    v.setTag(topicList.get(position1));
                    mOnItemClickListener.onItemClick(v, position1);
                }
            });
        }

        myViewHolder.name.setText(topicList.get(position).getModule());
    }

    @Override
    public int getItemCount() {
        return topicList.size();
    }

    /**
     * 新增模块信息
     *
     * @param moduleInfo module info
     */
    public void addModuleInfo(ModuleInfo moduleInfo) {
        topicList.add(moduleInfo);
        notifyDataSetChanged();
    }

    public void clear() {
        topicList.clear();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout list;
        TextView name;

        MyViewHolder(View itemView) {
            super(itemView);
            list = itemView.findViewById(R.id.list);
            name = itemView.findViewById(R.id.name);
        }
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }
}

