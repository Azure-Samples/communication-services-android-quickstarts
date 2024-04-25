package com.microsoft.acs.calling.rawvideo;

import android.util.Log;

import com.azure.android.communication.calling.RawOutgoingVideoStream;
import com.azure.android.communication.calling.RawVideoFrameBuffer;
import com.azure.android.communication.calling.VideoStreamState;

import java.util.HashSet;
import java.util.Set;

public abstract class CaptureService
{
    private static final String TAG = "CaptureService";

    private final Set<CaptureServiceListener> listeners;
    protected final RawOutgoingVideoStream stream;
    protected int orientation = 0;

    protected CaptureService(RawOutgoingVideoStream stream)
    {
        this.stream = stream;
        listeners = new HashSet<>();
    }

    protected void SendRawVideoFrame(RawVideoFrameBuffer rawVideoFrameBuffer)
    {
        if (CanSendRawVideoFrames())
        {
            try
            {
                Log.d(TAG, "SendRawVideoFrame trace, sending rawVideoFrame");
                stream.sendRawVideoFrame(rawVideoFrameBuffer).get();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }

            if (listeners.size() == 0)
            {
                Log.d(TAG, "SendRawVideoFrame trace, auto closing rawVideoFrame");
                rawVideoFrameBuffer.close();
            }
            else
            {
                for (CaptureServiceListener listener : listeners)
                {
                    listener.OnRawVideoFrameCaptured(rawVideoFrameBuffer, orientation);
                }
            }
        }
    }

    private boolean CanSendRawVideoFrames()
    {
        return stream != null &&
                stream.getFormat() != null &&
                stream.getState() == VideoStreamState.STARTED;
    }

    public void AddRawVideoFrameListener(CaptureServiceListener listener)
    {
        listeners.add(listener);
    }

    public void RemoveRawVideoFrameListener(CaptureServiceListener listener)
    {
        listeners.remove(listener);
    }
}

