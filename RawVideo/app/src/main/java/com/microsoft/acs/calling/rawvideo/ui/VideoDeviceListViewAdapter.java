package com.microsoft.acs.calling.rawvideo.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.azure.android.communication.calling.VideoDeviceInfo;
import com.microsoft.acs.calling.rawvideo.R;

import java.util.List;

public class VideoDeviceListViewAdapter extends ArrayAdapter<VideoDeviceInfo>
{
    public VideoDeviceListViewAdapter(Context context, List<VideoDeviceInfo> videoDeviceInfoList)
    {
        super(context, 0, videoDeviceInfoList);
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
            view = LayoutInflater.from(getContext()).inflate(R.layout.ui_layout_list_view_item_camera, parent, false);
        }

        VideoDeviceInfo cameraName = getItem(position);

        TextView cameraNameTextView = view.findViewById(R.id.camera_name);
        cameraNameTextView.setText(cameraName.getName());

        return view;
    }
}
