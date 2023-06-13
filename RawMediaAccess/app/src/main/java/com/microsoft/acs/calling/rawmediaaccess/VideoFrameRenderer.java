package com.microsoft.acs.calling.rawmediaaccess;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

import com.azure.android.communication.calling.RawVideoFrameBuffer;
import com.azure.android.communication.calling.VideoStreamFormat;

import java.nio.ByteBuffer;

public class VideoFrameRenderer implements SurfaceHolder.Callback
{
    private final Activity activity;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private int w = 0;
    private int h = 0;

    public VideoFrameRenderer(Activity activity)
    {
        this.activity = activity;

        surfaceView = activity.findViewById(R.id.IncomingVideoSurfaceView);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
    }

    public void RenderVideoFrame(RawVideoFrameBuffer rawVideoFrameBuffer)
    {
        if (surfaceView.getVisibility() == View.GONE)
        {
            activity.runOnUiThread(() -> surfaceView.setVisibility(View.VISIBLE));
        }

        VideoStreamFormat videoStreamFormat = rawVideoFrameBuffer.getStreamFormat();
        int w = videoStreamFormat.getWidth();
        int h = videoStreamFormat.getHeight();

        if (surfaceHolder != null && this.w != w && this.h != h)
        {
            this.w = w;
            this.h = h;
            surfaceHolder.setFixedSize(w, h);
        }

        try
        {
            RenderUsingSurfaceView(rawVideoFrameBuffer);
        }
        finally
        {
            rawVideoFrameBuffer.close();
        }
    }

    public void RenderUsingSurfaceView(RawVideoFrameBuffer rawVideoFrameBuffer)
    {
        ByteBuffer rgbaBuffer = rawVideoFrameBuffer.getBuffers().get(0);
        Bitmap rgbaBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        rgbaBitmap.copyPixelsFromBuffer(rgbaBuffer);

        if (surfaceHolder != null)
        {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null)
            {
                canvas.drawBitmap(rgbaBitmap, 0, 0, null);
            }

            if(surfaceHolder.getSurface().isValid())
            {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    public void ClearView()
    {
        if (surfaceHolder != null)
        {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null)
            {
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            }

            if(surfaceHolder.getSurface().isValid())
            {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

        activity.runOnUiThread(() -> surfaceView.setVisibility(View.GONE));
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder)
    {
        this.surfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height)
    {
        this.surfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder)
    {
        this.surfaceHolder = null;
    }
}
