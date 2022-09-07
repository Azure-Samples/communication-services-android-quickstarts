package com.microsoft.acs.calling.rawmediaaccess;

import android.util.Log;

import com.azure.android.communication.calling.SoftwareBasedVideoFrameSender;
import com.azure.android.communication.calling.VideoFormat;
import com.azure.android.communication.calling.VideoFrameSender;
import com.azure.android.communication.calling.VideoFrameSenderChangedEvent;
import com.azure.android.communication.calling.VideoFrameSenderChangedListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class FrameGenerator implements VideoFrameSenderChangedListener {

    private VideoFrameSender videoFrameSender;
    private Thread frameIteratorThread;
    private final Random random;
    private volatile boolean stopFrameIterator = false;

    public FrameGenerator() {

        random = new Random();
    }

    public void FrameIterator() {

        ByteBuffer plane = null;
        while (!stopFrameIterator && videoFrameSender != null) {

            plane = GenerateFrame(plane);
        }

        Log.d("FrameGenerator", "FrameGenerator.FrameIterator trace, 1st looper stopped");
    }

    private ByteBuffer GenerateFrame(ByteBuffer plane) {

        try {

            VideoFormat videoFormat = videoFrameSender.getVideoFormat();
            if (plane == null || videoFormat.getStride1() * videoFormat.getHeight() != plane.capacity()) {

                plane = ByteBuffer.allocateDirect(videoFormat.getStride1() * videoFormat.getHeight());
                plane.order(ByteOrder.nativeOrder());
            }

            int bandsCount = random.nextInt(15) + 1;
            int bandBegin = 0;
            int bandThickness = videoFormat.getHeight() * videoFormat.getStride1() / bandsCount;

            for (int i = 0; i < bandsCount; ++i) {

                byte greyValue = (byte) random.nextInt(254);
                java.util.Arrays.fill(plane.array(), bandBegin, bandBegin + bandThickness, greyValue);
                bandBegin += bandThickness;
            }

            SoftwareBasedVideoFrameSender sender = (SoftwareBasedVideoFrameSender) videoFrameSender;

            long timeStamp = sender.getTimestampInTicks();
            sender.sendFrame(plane, timeStamp).get();

            Thread.sleep((long) (1000.0f / videoFormat.getFramesPerSecond()));
        } catch (InterruptedException ex) {

            Log.d("FrameGenerator", String.format("FrameGenerator.GenerateFrame, %s", ex.getMessage()));
        } catch (ExecutionException ex2) {

            Log.d("FrameGenerator", String.format("FrameGenerator.GenerateFrame, %s", ex2.getMessage()));
        }

        return plane;
    }

    private void StartFrameIterator() {

        frameIteratorThread = new Thread(this::FrameIterator);
        frameIteratorThread.start();
    }

    public void StopFrameIterator() {

        try {

            if (frameIteratorThread != null) {

                stopFrameIterator = true;
                Log.d("FrameGenerator", "FrameGenerator.Stop trace, before stopping");

                frameIteratorThread.join();
                frameIteratorThread = null;

                Log.d("FrameGenerator", "FrameGenerator.Stop trace, after stopping");
                stopFrameIterator = false;
            }
        } catch (InterruptedException ex) {

            Log.d("FrameGenerator", String.format("FrameGenerator.StopFrameIterator, %s", ex.getMessage()));
        }
    }

    @Override
    public void onVideoFrameSenderChanged(VideoFrameSenderChangedEvent event) {

        if (event.getVideoFrameSender() == null) {

            Log.d("FrameGenerator", "FrameGenerator.onVideoFrameSenderChanged trace, video frame sender cleaned");
        } else {

            Log.d("FrameGenerator", "FrameGenerator.onVideoFrameSenderChanged trace, video frame sender set");
        }

        StopFrameIterator();
        this.videoFrameSender = event.getVideoFrameSender();
        StartFrameIterator();
    }
}
