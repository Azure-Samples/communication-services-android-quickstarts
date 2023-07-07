package com.microsoft.acs.calling.rawmediaaccess;

import com.azure.android.communication.calling.RawVideoFrame;

public interface RawVideoFrameListener
{
    void OnRawVideoFrameCaptured(RawVideoFrame rawVideoFrame);
}
