package com.zego.audioplayer.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zego.audioplayer.R;
import com.zego.audioplayer.entity.EffectInfo;

import java.util.ArrayList;
import java.util.List;

public class EffectListAdapter extends RecyclerView.Adapter {

    private List<EffectInfo> topicList = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.effect_list, parent, false);
        v.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    int position = (int) v.getTag();
                    mOnItemClickListener.onItemClick(v, position, topicList.get(position));
                }
            }
        });
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final MyViewHolder myViewHolder = (MyViewHolder) holder;
        myViewHolder.effectName.setText(topicList.get(position).effectName);
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return topicList.size();
    }

    /**
     * 新增音效列表信息
     *
     * @param effectInfoList
     */
    public void addEffectInfos(List<EffectInfo> effectInfoList) {
        topicList.clear();
        topicList.addAll(effectInfoList);
        notifyDataSetChanged();
    }

    public void clear() {
        topicList.clear();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView effectName;
        View itemView;

        MyViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.effectName = itemView.findViewById(R.id.effect_describe);

        }
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position, EffectInfo effectInfo);
    }
}

