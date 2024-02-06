package com.microsoft.acs.calling.rawvideo.ui;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.microsoft.acs.calling.rawvideo.R;

import java.util.List;

public class CameraListViewAdapter extends ArrayAdapter<String>
{
    private final CameraManager cameraManager;

    public CameraListViewAdapter(Context context, List<String> cameraList)
    {
        super(context, 0, cameraList);

        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
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

        String cameraId = getItem(position);

        int cameraFacing = CameraCharacteristics.LENS_FACING_EXTERNAL;
        try
        {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        String cameraFacingName = GetCameraFacingName(cameraFacing);

        TextView cameraNameTextView = view.findViewById(R.id.camera_name);
        cameraNameTextView.setText(String.format("%s %s", cameraId, cameraFacingName));

        return view;
    }

    private String GetCameraFacingName(int cameraFacing)
    {
        switch (cameraFacing)
        {
            case CameraCharacteristics.LENS_FACING_FRONT:
                return "Front";
            case CameraCharacteristics.LENS_FACING_BACK:
                return "Back";
            case CameraCharacteristics.LENS_FACING_EXTERNAL:
            default:
                return "Unknown";
        }
    }
}
