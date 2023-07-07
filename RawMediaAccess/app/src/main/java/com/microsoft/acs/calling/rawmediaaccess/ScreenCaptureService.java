package com.microsoft.acs.calling.rawmediaaccess;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.media.ImageReader;

import com.azure.android.communication.calling.RawOutgoingVideoStream;

import java.nio.ByteBuffer;

public class ScreenCaptureService extends CaptureService implements ImageReader.OnImageAvailableListener
{
    private final static Object locker;

    private static ImageReader.OnImageAvailableListener listener;
    private final Context context;
    private final int w;
    private final int h;
    private final int frameRate;
    private final int resultCode;
    private final Intent data;

    static
    {
        locker = new Object();
    }

    public ScreenCaptureService(Context context,
                                RawOutgoingVideoStream rawOutgoingVideoStream,
                                int w,
                                int h,
                                int frameRate,
                                int resultCode,
                                Intent data)
    {
        super(rawOutgoingVideoStream);

        this.context = context;
        this.w = w;
        this.h = h;
        this.frameRate = frameRate;
        this.resultCode = resultCode;
        this.data = data;
    }

    public void Start()
    {
        SetListener(this);

        Intent intent = new Intent(context, ScreenCaptureForegroundService.class);
        intent.setAction(Constants.START_SCREEN_CAPTURE);
        intent.putExtra(Constants.RESULT_CODE, resultCode);
        intent.putExtra(Constants.SCREEN_WIDTH, w);
        intent.putExtra(Constants.SCREEN_HEIGHT, h);
        intent.putExtra(Constants.FRAME_RATE, frameRate);
        intent.putExtra(Constants.EXTRA_DATA_INTENT, data);

        context.startForegroundService(intent);
    }

    public void Stop()
    {
        Intent intent = new Intent(context, ScreenCaptureForegroundService.class);
        intent.setAction(Constants.STOP_SCREEN_CAPTURE);

        context.stopService(intent);
    }

    @Override
    public void onImageAvailable(ImageReader reader)
    {
        Image image = reader.acquireLatestImage();
        if (image != null && image.getPlanes().length == 1)
        {
            try
            {
                ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();

                SendRawVideoFrame(byteBuffer);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            finally
            {
                image.close();
            }
        }
    }

    public static void SetListener(ImageReader.OnImageAvailableListener newListener)
    {
        synchronized (locker)
        {
            listener = newListener;
        }
    }

    public static ImageReader.OnImageAvailableListener GetListener()
    {
        synchronized (locker)
        {
            return listener;
        }
    }
}
