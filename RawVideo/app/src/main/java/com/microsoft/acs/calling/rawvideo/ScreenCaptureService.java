package com.microsoft.acs.calling.rawvideo;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.util.Log;

import com.azure.android.communication.calling.RawOutgoingVideoStream;
import com.azure.android.communication.calling.RawVideoFrameBuffer;
import com.azure.android.communication.calling.VideoStreamFormat;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ScreenCaptureService extends CaptureService implements ImageReader.OnImageAvailableListener
{
    private static final String TAG = "ScreenCaptureService";

    private static ImageReader.OnImageAvailableListener listener;
    private final static Object locker;

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
        Log.d(TAG, "Start");
        SetListener(this);

        Intent intent = new Intent(context, ScreenCaptureForegroundService.class);
        intent.setAction(Constants.START_SCREEN_CAPTURE);
        intent.putExtra(Constants.RESULT_CODE, resultCode);
        intent.putExtra(Constants.SCREEN_WIDTH, w);
        intent.putExtra(Constants.SCREEN_HEIGHT, h);
        intent.putExtra(Constants.FRAME_RATE, frameRate);
        intent.putExtra(Constants.EXTRA_DATA_INTENT, data);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            context.startForegroundService(intent);
        }
        else
        {
            context.startService(intent);
        }
    }

    public void Stop()
    {
        Log.d(TAG, "Stop");

        Intent intent = new Intent(context, ScreenCaptureForegroundService.class);
        intent.setAction(Constants.STOP_SCREEN_CAPTURE);

        context.stopService(intent);
    }

    @Override
    public void onImageAvailable(ImageReader reader)
    {
        Image image = reader.acquireNextImage();
        if (image != null && image.getPlanes().length == 1)
        {
            try
            {
                Image.Plane plane = image.getPlanes()[0];
                ByteBuffer byteBuffer = plane.getBuffer();

                if (byteBuffer != null)
                {
                    VideoStreamFormat format = rawOutgoingVideoStream.getFormat();
                    format.setStride1(plane.getRowStride());

                    RawVideoFrameBuffer rawVideoFrameBuffer = new RawVideoFrameBuffer()
                            .setBuffers(Arrays.asList(byteBuffer))
                            .setStreamFormat(format);

                    SendRawVideoFrame(rawVideoFrameBuffer);
                }
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

    public static void SetListener(ImageReader.OnImageAvailableListener listener)
    {
        synchronized (locker)
        {
            ScreenCaptureService.listener = listener;
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
