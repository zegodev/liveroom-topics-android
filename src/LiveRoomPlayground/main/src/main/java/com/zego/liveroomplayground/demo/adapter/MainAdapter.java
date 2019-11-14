package com.zego.liveroomplayground.demo.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.zego.liveroomplayground.R;
import com.zego.liveroomplayground.demo.entity.ModuleInfo;

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
        ModuleInfo moduleInfo = topicList.get(position);
        if (moduleInfo.getTitleName() != null) {
            myViewHolder.title.setVisibility(View.VISIBLE);
            myViewHolder.titleName.setText(moduleInfo.getTitleName());
        } else {
            myViewHolder.title.setVisibility(View.GONE);
        }

        myViewHolder.name.setText(moduleInfo.getModule());
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
        TextView titleName;
        RelativeLayout title;

        MyViewHolder(View itemView) {
            super(itemView);
            list = itemView.findViewById(R.id.list);
            name = itemView.findViewById(R.id.name);
            titleName = itemView.findViewById(R.id.title_name);
            title = itemView.findViewById(R.id.title);
        }
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }
}

