package com.microsoft.acs.calling.rawmediaaccess;

import static android.view.Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

public class ScreenCaptureForegroundService extends Service
{
    private WindowManager windowManager;
    private Surface surface;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private OrientationEventListener orientationEventListener;
    private MediaProjectionCallBack mediaProjectionCallBack;

    private int displayOrientation;
    private int w;
    private int h;
    private float frameRate;
    private int pixelDensity;
    private String channelName = "Name-001";
    private String channelId = "001";
    private int notificationId = 100;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mediaProjectionCallBack = new MediaProjectionCallBack(this);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent == null)
        {
            Stop();
        }
        else
        {
            String action = intent.getAction();
            if (action != null)
            {
                switch (action)
                {
                    case Constants.START_SCREEN_CAPTURE:
                        Start(intent);
                        break;
                    case Constants.STOP_SCREEN_CAPTURE:
                        Stop();
                        break;
                    default:
                        Log.d("ScreenCaptureForegroundService", "ScreenCaptureForegroundService.onStartCommand trace, unknown action");
                        break;
                }
            }
        }

        return START_STICKY;
    }

    private void Start(Intent intent)
    {
        int resultCode = intent.getIntExtra(Constants.RESULT_CODE, Activity.RESULT_CANCELED);
        w = intent.getIntExtra(Constants.SCREEN_WIDTH, 0);
        h = intent.getIntExtra(Constants.SCREEN_HEIGHT, 0);
        frameRate = intent.getIntExtra(Constants.FRAME_RATE, 0);
        Intent intentData = intent.getParcelableExtra(Constants.EXTRA_DATA_INTENT);

        Dispose();
        ShowForegroundServiceNotification();
        SetDisplayValues();
        Initialize(resultCode, intentData);
    }

    private void Stop()
    {
        Dispose();
        stopForeground(true);
        stopSelf();
    }

    private void Initialize(int resultCode, Intent intentData)
    {
        CreateSurface();

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, intentData);

        Handler handler = new Handler(getApplication().getMainLooper());
        mediaProjection.registerCallback(mediaProjectionCallBack, handler);

        int flags = DisplayManager.MATCH_CONTENT_FRAMERATE_ALWAYS;
        virtualDisplay = mediaProjection.createVirtualDisplay(
                Constants.SCREEN_SHARE_VIRTUAL_DISPLAY,
                w,
                h,
                pixelDensity,
                flags,
                surface,
                null,
                null);
        virtualDisplay.setSurface(surface);

        orientationEventListener = new OrientationBridgeListener(this);
        orientationEventListener.enable();
    }

    private void SetDisplayValues()
    {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        pixelDensity = metrics.densityDpi;
        displayOrientation = display.getRotation();
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void ShowForegroundServiceNotification()
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Foreground Service is running.");
        notificationManager.createNotificationChannel(channel);

        Intent contentIntent = new Intent(getBaseContext(), MainActivity.class);
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat
                .Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Screens Capture")
                .setContentText("Foreground Service is running.")
                .setContentIntent(pendingIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setAutoCancel(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        }
        else
        {
            startForeground(notificationId, notification);
        }
    }

    @SuppressWarnings({"WrongConstant"})
    private void CreateSurface()
    {
        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(ScreenCaptureService.GetListener(), null);
        surface = imageReader.getSurface();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            try
            {
                surface.setFrameRate(frameRate, FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    @SuppressWarnings({"WrongConstant", "SuspiciousNameCombination"})
    public void SwapDisplayValues()
    {
        CleanImageReader();

        int currentWidth = w;
        w = h;
        h = currentWidth;

        CreateSurface();

        if (virtualDisplay != null) {

            virtualDisplay.resize(w, h, pixelDensity);
            virtualDisplay.setSurface(surface);
        }
    }

    private void CleanImageReader()
    {
        if (surface != null)
        {
            surface.release();
            surface = null;
        }

        if (imageReader != null)
        {
            imageReader.close();
            imageReader = null;
        }
    }

    private void Dispose()
    {
        CleanImageReader();
        if (virtualDisplay != null)
        {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (mediaProjection != null)
        {
            mediaProjection.stop();
            mediaProjection.unregisterCallback(mediaProjectionCallBack);
            mediaProjection = null;
        }

        if (orientationEventListener != null)
        {
            orientationEventListener.disable();
            orientationEventListener = null;
        }
    }

    private static class OrientationBridgeListener extends OrientationEventListener
    {
        private final ScreenCaptureForegroundService service;

        public OrientationBridgeListener(ScreenCaptureForegroundService service)
        {
            super(service);
            this.service = service;
        }

        @Override
        public void onOrientationChanged(int i)
        {
            int rotation = service.windowManager.getDefaultDisplay().getRotation();
            if (rotation != service.displayOrientation)
            {
                if (Math.abs(service.displayOrientation - rotation) % 2 != 0)
                {
                    service.SwapDisplayValues();
                }

                service.displayOrientation = rotation;
            }
        }
    }

    private static class MediaProjectionCallBack extends MediaProjection.Callback
    {
        private final ScreenCaptureForegroundService service;

        public MediaProjectionCallBack(ScreenCaptureForegroundService service)
        {
            this.service = service;
        }

        public void onStop()
        {
            service.Stop();
        }
    }
}
