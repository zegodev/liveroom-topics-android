package com.zego.joinlive.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zego.joinlive.R;
import com.zego.support.RoomInfo;

import java.util.ArrayList;
import java.util.List;

public class AudienceListAdapter extends RecyclerView.Adapter {

    private List<String> topicList = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.audience_list, parent, false);
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
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final MyViewHolder myViewHolder = (MyViewHolder) holder;
        myViewHolder.audienceID.setText(topicList.get(position));
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return topicList.size();
    }

    /**
     * 新增观众列表信息
     *
     * @param audienceList
     */
    public void addAudienceInfo(List<String> audienceList) {
        topicList.clear();
        topicList.addAll(audienceList);
        notifyDataSetChanged();
    }

    public void clear() {
        topicList.clear();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView audienceID;
        View itemView;

        MyViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.audienceID = itemView.findViewById(R.id.audienceID);
        }
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position, String audienceID);
    }

}






