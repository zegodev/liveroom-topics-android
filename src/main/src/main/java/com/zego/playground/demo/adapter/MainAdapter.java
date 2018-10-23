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

    List<ModuleInfo> roomList = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.module_list, parent, false);

        MyViewHolder viewHolder = new MyViewHolder(v);


        return viewHolder;
    }


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final MyViewHolder myViewHolder = (MyViewHolder) holder;
        if (mOnItemClickListener != null) {
            myViewHolder.list.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    int position1 = myViewHolder.getLayoutPosition();
                    v.setTag(roomList.get(position1));
                    mOnItemClickListener.onItemClick(v, position1);
                }
            });
        }

        myViewHolder.name.setText(roomList.get(position).getModule());

    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    /**
     * 新增模块信息
     *
     * @param moduleInfo
     */
    public void addModuleInfo(ModuleInfo moduleInfo) {
        roomList.add(moduleInfo);
        notifyDataSetChanged();
    }

    public void clear() {
        roomList.clear();
    }


    public static class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout list;
        TextView name;

        public MyViewHolder(View itemView) {
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

