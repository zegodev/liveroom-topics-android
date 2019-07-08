package com.zego.common.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.zego.common.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by zego on 2018/2/6.
 */

public class LogAdapter extends RecyclerView.Adapter {

    private List<String> logList = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final MyViewHolder myViewHolder = (MyViewHolder) holder;

        myViewHolder.name.setText(logList.get(position));
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }


    public void addLog(final String log) {
        Log.e("addLog", " " + logList.size());
        logList.add(log);
        // 防止日志太多。当日志超过1000条就清空500条
        if (logList.size() > 1000) {
            List<String> tempList = new ArrayList<>(logList.subList(500, 1000));
            logList.clear();
            logList = tempList;
        }

        notifyDataSetChanged();

    }


    public void clear() {

        logList.clear();
        notifyDataSetChanged();
    }


    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        MyViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.log);
        }
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }
}

