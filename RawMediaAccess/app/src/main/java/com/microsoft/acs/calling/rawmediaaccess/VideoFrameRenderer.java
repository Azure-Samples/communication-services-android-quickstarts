package com.microsoft.acs.calling.rawmediaaccess;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;

import com.azure.android.communication.calling.RawVideoFrameBuffer;
import com.azure.android.communication.calling.VideoStreamFormat;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoFrameRenderer implements SurfaceHolder.Callback
{
    private final Activity activity;
    private final ConstraintLayout parentView;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private int w = 0;
    private int h = 0;
    private int viewDpW;
    private int viewDpH;

    public VideoFrameRenderer(Activity activity, ConstraintLayout parentView, int viewW, int viewH)
    {
        this.activity = activity;
        this.parentView = parentView;

        viewDpW = ConvertDpToPixel(viewW);
        viewDpH = ConvertDpToPixel(viewH);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                viewDpW,
                viewDpH);

        surfaceView = new SurfaceView(activity);
        surfaceView.setLayoutParams(params);

        parentView.addView(surfaceView);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setFixedSize(viewDpW, viewDpH);
    }

    public void RenderRawVideoFrame(RawVideoFrameBuffer rawVideoFrameBuffer)
    {
        VideoStreamFormat videoStreamFormat = rawVideoFrameBuffer.getStreamFormat();
        this.w = videoStreamFormat.getWidth();
        this.h = videoStreamFormat.getHeight();

        RenderUsingSurfaceView(rawVideoFrameBuffer);
    }

    public void RenderUsingSurfaceView(RawVideoFrameBuffer rawVideoFrameBuffer)
    {
        ByteBuffer rgbaBuffer = rawVideoFrameBuffer.getBuffers().get(0);
        Bitmap rgbaBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        rgbaBitmap.copyPixelsFromBuffer(rgbaBuffer);

        float scaleFactor = ((float) viewDpW) / w;
        if (h > viewDpH || (h * scaleFactor) > viewDpH)
        {
            scaleFactor = ((float) viewDpH) / h;
        }

        int leftOffset = (int)((viewDpW - (w * scaleFactor)) / 2);

        Matrix matrix = new Matrix();
        matrix.postScale(scaleFactor, scaleFactor);

        Bitmap resizedBitmap = Bitmap.createBitmap(rgbaBitmap, 0, 0, w, h, matrix, false);
        rgbaBitmap.recycle();

        if (surfaceHolder != null)
        {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null)
            {
                canvas.drawBitmap(resizedBitmap, leftOffset, 0, null);
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

    public int ConvertDpToPixel(int dp)
    {
        return (int) (dp * ((float)
                activity.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
