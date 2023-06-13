package com.microsoft.acs.calling.rawmediaaccess;

import java.nio.ByteBuffer;

public interface IScreenCaptureListener {

    void onScreenShareServiceStateChangedEvent(IScreenCaptureServiceStateEvent event);

    void onFrameReady(ByteBuffer buffer, int width, int height, int rowStride, int pixelStride, int rotation);
}
