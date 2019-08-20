package com.zego.common.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zego.common.R;

import java.util.LinkedList;

/**
 * <p>Copyright © 2017 Zego. All rights reserved.</p>
 *
 * @author realuei on 26/10/2017.
 */

public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.MyViewHolder> {
    private LinkedList<String> mData;

    private LayoutInflater mLayoutInflater;

    public LogListAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public LogListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(mLayoutInflater.inflate(R.layout.vt_widget_log_item, parent, false));
    }

    @Override
    public void onBindViewHolder(LogListAdapter.MyViewHolder holder, int position) {
        holder.tv.setText(mData.get(position));

        holder.tv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        holder.tv.requestFocus();
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public void setData(LinkedList<String> data) {
        mData = data;
        notifyDataSetChanged();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tv;

        public MyViewHolder(View view) {
            super(view);
            tv = (TextView) view.findViewById(R.id.vt_text_view);
        }
    }
}
