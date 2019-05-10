package com.zego.sound.processing.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zego.sound.processing.R;
import com.zego.support.RoomInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zego on 2018/2/6.
 */

public class RoomListAdapter extends RecyclerView.Adapter {

    private List<RoomInfo> topicList = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.room_list, parent, false);
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
        myViewHolder.roomName.setText("roomID: " + topicList.get(position).getRoomId());
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return topicList.size();
    }

    /**
     * 新增房间列表信息
     *
     * @param roomInfoList
     */
    public void addRoomInfo(List<RoomInfo> roomInfoList) {
        topicList.clear();
        topicList.addAll(roomInfoList);
        notifyDataSetChanged();
    }

    public void clear() {
        topicList.clear();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView roomName;
        View itemView;

        MyViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.roomName = itemView.findViewById(R.id.name);

        }
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position, RoomInfo roomInfo);
    }
}

