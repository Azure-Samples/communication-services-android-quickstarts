package com.microsoft.acs.calling.rawmediaaccess;

import com.azure.android.communication.calling.RawOutgoingVideoStream;
import com.azure.android.communication.calling.RawVideoFrame;
import com.azure.android.communication.calling.RawVideoFrameBuffer;
import com.azure.android.communication.calling.VideoStreamState;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class CaptureService
{
    protected final RawOutgoingVideoStream rawOutgoingVideoStream;

    protected CaptureService(RawOutgoingVideoStream rawOutgoingVideoStream)
    {
        this.rawOutgoingVideoStream = rawOutgoingVideoStream;
    }

    protected void SendRawVideoFrame(ByteBuffer byteBuffer)
    {
        if (byteBuffer != null && CanSendRawVideoFrames())
        {
            RawVideoFrame rawVideoFrame = new RawVideoFrameBuffer()
                    .setBuffers(Arrays.asList(byteBuffer))
                    .setStreamFormat(rawOutgoingVideoStream.getFormat());

            try
            {
                rawOutgoingVideoStream.sendRawVideoFrame(rawVideoFrame).get();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            finally
            {
                rawVideoFrame.close();
            }
        }
    }

    private boolean CanSendRawVideoFrames()
    {
        return rawOutgoingVideoStream != null &&
                rawOutgoingVideoStream.getFormat() != null &&
                rawOutgoingVideoStream.getState() == VideoStreamState.STARTED;
    }
}
