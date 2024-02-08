package com.microsoft.acs.calling.rawvideo;

import com.azure.android.communication.calling.RawVideoFrameBuffer;

public interface CaptureServiceListener
{
    void OnRawVideoFrameCaptured(RawVideoFrameBuffer rawVideoFrameBuffer, int orientation);
}
