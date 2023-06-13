package com.microsoft.acs.calling.rawmediaaccess;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.azure.android.communication.calling.RawOutgoingVideoStream;
import com.azure.android.communication.calling.RawVideoFrame;
import com.azure.android.communication.calling.RawVideoFrameBuffer;
import com.azure.android.communication.calling.VideoStreamState;

import java.nio.ByteBuffer;
import java.util.Collections;

public class ScreenShareService implements IScreenCaptureListener
{
    private final Context context;
    private final Object locker;
    private RawOutgoingVideoStream rawOutgoingVideoStream;
    private final int w;
    private final int h;
    private final int frameRate;

    public ScreenShareService(Context context, RawOutgoingVideoStream rawOutgoingVideoStream, int w, int h, int frameRate)
    {
        this.context = context;
        this.rawOutgoingVideoStream = rawOutgoingVideoStream;
        this.w = w;
        this.h = h;
        this.frameRate = frameRate;
        locker = new Object();
    }

    public void Start(int resultCode, Intent dataIntent)
    {
        ScreenCaptureListenerBridge.SetListener(this);

        Intent intent = new Intent(context, ScreenCaptureForegroundService.class);
        intent.setAction(Constants.START_SCREEN_CAPTURE);
        intent.putExtra(Constants.RESULT_CODE, resultCode);
        intent.putExtra(Constants.SCREEN_WIDTH, w);
        intent.putExtra(Constants.SCREEN_HEIGHT, h);
        intent.putExtra(Constants.FRAME_RATE, frameRate);
        intent.putExtra(Constants.EXTRA_DATA_INTENT, dataIntent);

        context.startForegroundService(intent);
    }

    public void Stop()
    {
        Intent intent = new Intent(context, ScreenCaptureForegroundService.class);
        intent.setAction(Constants.STOP_SCREEN_CAPTURE);

        context.stopService(intent);
    }

    @Override
    public void onScreenShareServiceStateChangedEvent(IScreenCaptureServiceStateEvent event)
    {
        if (event.GetMessage() != null)
        {
            Log.d("MainActivity",
                    String.format("MainActivity.onScreenCaptureStateChanged, state: %s, trace: %s",
                            event.GetScreenCaptureServiceState().toString(),
                            event.GetMessage()));
        }
    }

    @Override
    public void onFrameReady(ByteBuffer buffer, int width, int height, int rowStride, int pixelStride, int rotation)
    {
        if (CanSendFrames())
        {
            synchronized (locker)
            {
                RawVideoFrame rawVideoFrame = CreateRawVideoFrameBufferRGBA(buffer);
                try
                {
                    rawOutgoingVideoStream.sendRawVideoFrame(rawVideoFrame).get();
                }
                catch (Exception ex)
                {

                }
                finally
                {
                    rawVideoFrame.close();
                }
            }
        }
    }

    private RawVideoFrame CreateRawVideoFrameBufferRGBA(ByteBuffer rgbaBuffer)
    {
        RawVideoFrameBuffer rawVideoFrameBuffer = new RawVideoFrameBuffer();
        rawVideoFrameBuffer.setBuffers(Collections.singletonList(rgbaBuffer));
        rawVideoFrameBuffer.setStreamFormat(rawOutgoingVideoStream.getFormat());

        return rawVideoFrameBuffer;
    }

    private boolean CanSendFrames()
    {
        return rawOutgoingVideoStream != null &&
                rawOutgoingVideoStream.getFormat() != null &&
                rawOutgoingVideoStream.getState() == VideoStreamState.STARTED;
    }
}
