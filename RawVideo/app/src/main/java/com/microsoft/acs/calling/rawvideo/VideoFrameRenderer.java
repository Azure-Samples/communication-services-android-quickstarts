package com.microsoft.acs.calling.rawvideo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.azure.android.communication.calling.RawVideoFrameBuffer;
import com.azure.android.communication.calling.ScalingMode;
import com.azure.android.communication.calling.VideoStreamFormat;

import java.nio.ByteBuffer;

public class VideoFrameRenderer implements SurfaceHolder.Callback
{
    private static final String TAG = "VideoFrameRenderer";

    private final Activity activity;
    private final SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ScalingMode scalingMode;
    private int w = 0;
    private int h = 0;
    private float scaledW = 0;
    private float scaledH = 0;
    private final boolean invertSize;

    public VideoFrameRenderer(Activity activity, int viewW, int viewH, ScalingMode scalingMode, boolean invertSize, boolean setOnTop)
    {
        this.activity = activity;
        this.scalingMode = scalingMode;
        this.invertSize = invertSize;

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                DpToPixel(viewW),
                DpToPixel(viewH));

        surfaceView = new SurfaceView(activity);
        surfaceView.setLayoutParams(params);
        surfaceView.setZOrderOnTop(setOnTop);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
    }

    public void RenderRawVideoFrame(RawVideoFrameBuffer rawVideoFrameBuffer)
    {
        RenderRawVideoFrame(rawVideoFrameBuffer, 0);
    }

    public void RenderRawVideoFrame(RawVideoFrameBuffer rawVideoFrameBuffer, int orientation)
    {
        VideoStreamFormat format = rawVideoFrameBuffer.getStreamFormat();
        this.w = format.getStride1() / 4;
        this.h = format.getHeight();

        ByteBuffer rgbaBuffer = rawVideoFrameBuffer.getBuffers().get(0);
        Bitmap rgbaBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        rgbaBitmap.copyPixelsFromBuffer(rgbaBuffer);

        Matrix matrix = CreateMatrix(w, h, orientation);

        Bitmap resizedBitmap = Bitmap.createBitmap(rgbaBitmap, 0, 0, w, h, matrix, false);
        rgbaBitmap.recycle();

        DrawBitmap(resizedBitmap);
    }

    private void DrawBitmap(Bitmap bitmap)
    {
        if (surfaceHolder != null)
        {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null)
            {
                float leftOffset = (canvas.getWidth() - scaledW) / 2f;
                float topOffset = (canvas.getHeight() - scaledH) / 2f;

                Log.d(TAG, String.format("Setting resized bitmap with leftOffset: %f, topOffset: %f",
                        leftOffset,
                        topOffset));

                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(bitmap, leftOffset, topOffset, null);
                bitmap.recycle();
            }

            if(surfaceHolder.getSurface().isValid())
            {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private Matrix CreateMatrix(int w, int h, int orientation)
    {
        if (invertSize)
        {
            int temp = w;
            w = h;
            h = temp;
        }

        Log.d(TAG, String.format("Calculation parameters, view w: %d x h: %d, video frame w: %d x h: %d, orientation: %d",
                surfaceView.getWidth(),
                surfaceView.getHeight(),
                w,
                h,
                orientation));

        float viewW = (float) surfaceView.getWidth();
        float viewH = (float) surfaceView.getHeight();
        float scaleFactorX = viewW / (float) w;
        float scaleFactorY = viewH / (float) h;
        float scale = 0f;

        switch (scalingMode)
        {
            case FIT:
                Log.d(TAG, "Fitting video frame");
                scale = Math.min(scaleFactorX, scaleFactorY);
                break;
            case CROP:
                Log.d(TAG, "Cropping video frame");
                scale = Math.max(scaleFactorX, scaleFactorY);
                break;
        }

        float scaledW = scale * w;
        float scaledH = scale * h;

        Log.d(TAG, String.format("Scale factor: %f, scaled w: %f x h: %f",
                scale,
                scaledW,
                scaledH));

        Matrix matrix = new Matrix();
        matrix.setRotate(orientation);
        matrix.postScale(scale, scale, w / 2f, h / 2f);

        this.scaledW = scaledW;
        this.scaledH = scaledH;

        return matrix;
    }

    public void ClearView()
    {
        if (surfaceHolder != null)
        {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null)
            {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
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
        Log.d(TAG, "surfaceCreated");
        this.surfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h)
    {
        Log.d(TAG, String.format("surfaceChanged w: %d x h: %d", w, h));
        this.surfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder)
    {
        Log.d(TAG, "surfaceDestroyed");
        this.surfaceHolder = null;
    }

    public int DpToPixel(int dp)
    {
        return (int) (dp * ((float)
                activity.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public SurfaceView GetView()
    {
        return surfaceView;
    }
}
