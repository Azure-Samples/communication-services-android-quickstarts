package com.microsoft.acs.calling.rawvideo.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.azure.android.communication.calling.VideoStreamType;
import com.microsoft.acs.calling.rawvideo.R;

import java.util.List;

public class VideoStreamTypeListViewAdapter extends ArrayAdapter<VideoStreamType>
{
    public VideoStreamTypeListViewAdapter(Context context, List<VideoStreamType> videoStreamTypeList)
    {
        super(context, 0, videoStreamTypeList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        return CreateView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent)
    {
        return CreateView(position, convertView, parent);
    }

    private View CreateView(int position, View convertView, ViewGroup parent)
    {
        View view = convertView;
        if (view == null)
        {
            view = LayoutInflater.from(getContext()).inflate(R.layout.ui_layout_list_view_item_video_stream_type, parent, false);
        }

        VideoStreamType videoStreamType = getItem(position);

        TextView videoStreamTypeTextView = view.findViewById(R.id.video_stream_type);
        videoStreamTypeTextView.setText(videoStreamType.toString());

        return view;
    }
}
