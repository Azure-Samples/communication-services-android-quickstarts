package com.microsoft.acs.calling.rawmediaaccess;

import android.content.Context;
import android.widget.Toast;

import com.azure.android.communication.calling.RawOutgoingVideoStream;
import com.azure.android.communication.calling.RawVideoFrame;
import com.azure.android.communication.calling.RawVideoFrameBuffer;
import com.azure.android.communication.calling.VideoStreamFormat;
import com.azure.android.communication.calling.VideoStreamState;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Random;

public class VideoFrameSender
{
    private Context context;
    private RawOutgoingVideoStream rawOutgoingVideoStream;
    private Thread frameIteratorThread;
    private final Random random = new Random();
    private volatile boolean stopFrameIterator = false;

    public VideoFrameSender(Context context, RawOutgoingVideoStream rawOutgoingVideoStream)
    {
        this.context = context;
        this.rawOutgoingVideoStream = rawOutgoingVideoStream;
    }

    public void VideoFrameIterator()
    {
        while (!stopFrameIterator)
        {
            if (CanSendFrames())
            {
                SendRandomVideoFrame();
            }
        }
    }

    private void SendRandomVideoFrame()
    {
        RawVideoFrame rawVideoFrame = GenerateRawVideoFrame();
        try
        {
            rawOutgoingVideoStream.sendRawVideoFrame(rawVideoFrame).get();

            int delayBetweenFrames = (int)(1000.0 /
                    rawOutgoingVideoStream.getFormat().getFramesPerSecond());
            Thread.sleep(delayBetweenFrames);
        }
        catch (Exception ex)
        {
            Toast.makeText(context,
                            "Unexpected error while sending RawVideoFrame",
                            Toast.LENGTH_SHORT)
                    .show();
        }
        finally
        {
            rawVideoFrame.close();
        }
    }

    private RawVideoFrame GenerateRawVideoFrame()
    {
        VideoStreamFormat videoStreamFormat = rawOutgoingVideoStream.getFormat();
        int w = videoStreamFormat.getWidth();
        int h = videoStreamFormat.getHeight();
        int rgbaCapacity = w * h * 4;

        ByteBuffer rgbaBuffer = ByteBuffer.allocateDirect(rgbaCapacity);
        rgbaBuffer.order(ByteOrder.nativeOrder());

        int bandsCount = random.nextInt(15) + 1;
        int bandBegin = 0;
        int bandThickness = h * w * 4 / bandsCount;

        for (int i = 0; i < bandsCount; ++i)
        {
            byte greyValue = (byte) random.nextInt(254);
            java.util.Arrays.fill(rgbaBuffer.array(), bandBegin, bandBegin + bandThickness, greyValue);
            bandBegin += bandThickness;
        }

        rgbaBuffer.rewind();

        RawVideoFrameBuffer rawVideoFrameBuffer = new RawVideoFrameBuffer();
        rawVideoFrameBuffer.setBuffers(Collections.singletonList(rgbaBuffer));
        rawVideoFrameBuffer.setStreamFormat(rawOutgoingVideoStream.getFormat());

        return rawVideoFrameBuffer;
    }

    public void Start()
    {
        frameIteratorThread = new Thread(this::VideoFrameIterator);
        frameIteratorThread.start();
    }

    public void Stop()
    {
        try
        {
            if (frameIteratorThread != null)
            {
                stopFrameIterator = true;

                frameIteratorThread.join();
                frameIteratorThread = null;

                stopFrameIterator = false;
            }
        }
        catch (InterruptedException ex)
        {
            Toast.makeText(context,
                            "Unexpected error while stopping VideoFrameSender",
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    private boolean CanSendFrames()
    {
        return rawOutgoingVideoStream != null &&
                rawOutgoingVideoStream.getFormat() != null &&
                rawOutgoingVideoStream.getState() == VideoStreamState.STARTED;
    }
}
