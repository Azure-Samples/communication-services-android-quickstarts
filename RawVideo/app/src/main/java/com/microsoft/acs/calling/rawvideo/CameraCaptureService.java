package com.microsoft.acs.calling.rawvideo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.azure.android.communication.calling.RawOutgoingVideoStream;
import com.azure.android.communication.calling.RawVideoFrameBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraCaptureService extends CaptureService implements ImageReader.OnImageAvailableListener
{
    private static final String TAG = "CameraCaptureService";

    private final Activity context;
    private final String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private Handler backgroundHandler;
    private HandlerThread backgroundHandlerThread;
    private Surface surface;
    private ImageReader imageReader;
    private final int w;
    private final int h;

    public CameraCaptureService(Activity context,
                                RawOutgoingVideoStream rawOutgoingVideoStream,
                                String cameraId,
                                int w,
                                int h)
    {
        super(rawOutgoingVideoStream);

        this.context = context;
        this.cameraId = cameraId;
        this.w = w;
        this.h = h;
    }

    @SuppressLint("MissingPermission")
    private void OpenCamera()
    {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try
        {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback()
            {
                @Override
                public void onOpened(@NonNull CameraDevice _cameraDevice)
                {
                    cameraDevice = _cameraDevice;
                    AttachSurface();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice _cameraDevice)
                {
                    _cameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice _cameraDevice, int i)
                {
                    _cameraDevice.close();
                }

                @Override
                public void onClosed(@NonNull CameraDevice _cameraDevice)
                {

                }
            }, backgroundHandler);
        }
        catch (CameraAccessException ex)
        {
            ex.printStackTrace();
        }
    }

    private void CloseCamera()
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

        if (cameraDevice != null)
        {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (cameraCaptureSession != null)
        {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader)
    {
        Image image = reader.acquireNextImage();
        if (image != null && image.getPlanes().length == 3)
        {
            try
            {
                ByteBuffer byteBuffer = ConvertI420ToRGBA(image, 0);

                if (byteBuffer != null)
                {
                    RawVideoFrameBuffer rawVideoFrameBuffer = new RawVideoFrameBuffer()
                            .setBuffers(Arrays.asList(byteBuffer))
                            .setStreamFormat(rawOutgoingVideoStream.getFormat());

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

    private ByteBuffer ConvertI420ToRGBA(Image image, int orientation)
    {
        Image.Plane[] planes = image.getPlanes();

        int w = image.getWidth();
        int h = image.getHeight();
        int rgbaStride = 0;
        int rgbaPixelStride = 4;
        int rgbaCapacity = w * h * rgbaPixelStride;

        Image.Plane yPlane = planes[0];
        Image.Plane uPlane = planes[1];
        Image.Plane vPlane = planes[2];

        byte[] yArrayBuffer = BufferExtensions.GetArrayBuffer(yPlane.getBuffer());
        byte[] uArrayBuffer = BufferExtensions.GetArrayBuffer(uPlane.getBuffer());
        byte[] vArrayBuffer = BufferExtensions.GetArrayBuffer(vPlane.getBuffer());
        byte[] rgbaArrayBuffer = new byte[rgbaCapacity];

        int halfY, halfX, yVal, uVal, vVal, rgbaStart, newY = 0, newX = 0;

        for (int y = 0; y < h; ++y)
        {
            for (int x = 0; x < w; x++)
            {
                halfY = y / 2;
                halfX = x / 2;

                yVal = yArrayBuffer[(y * w) + x] & 0xFF;
                uVal = uArrayBuffer[(halfY * uPlane.getRowStride()) + (halfX * uPlane.getPixelStride())] & 0xFF;
                vVal = vArrayBuffer[(halfY * vPlane.getRowStride()) + (halfX * vPlane.getPixelStride())] & 0xFF;

                switch (orientation)
                {
                    case 0:
                        rgbaStride = w * rgbaPixelStride;
                        newX = x;
                        newY = y;
                        break;
                    case 90:
                        rgbaStride = h * rgbaPixelStride;
                        newX = h - 1 - y;
                        newY = x;
                        break;
                    case 180:
                        rgbaStride = w * rgbaPixelStride;
                        newX = h - 1 - x;
                        newY = w - 1 - y;
                        break;
                    case 270:
                        rgbaStride = h * rgbaPixelStride;
                        newX = y;
                        newY = w - 1 - x;
                        break;
                }

                rgbaStart = (newY * rgbaStride) + (newX * rgbaPixelStride);

                ConvertYUVToRGBA(rgbaArrayBuffer, yVal, uVal, vVal, rgbaStart);
            }
        }

        ByteBuffer rgbaBuffer = ByteBuffer.allocateDirect(rgbaCapacity);
        rgbaBuffer.order(ByteOrder.nativeOrder());
        rgbaBuffer.put(rgbaArrayBuffer);
        rgbaBuffer.rewind();

        return rgbaBuffer;
    }

    private void ConvertYUVToRGBA(byte[] rgbaArrayBuffer, int yVal, int uVal, int vVal, int rgbaStart)
    {
        yVal -= 16;
        uVal -= 128;
        vVal -= 128;

        double rVal = 1.164 * yVal + 1.596 * vVal;
        double gVal = 1.164 * yVal - 0.183 * vVal - 0.392 * uVal;
        double bVal = 1.164 * yVal + 2.018 * uVal;
        double aVal = 255;

        rgbaArrayBuffer[rgbaStart + 0] = Clip(rVal);
        rgbaArrayBuffer[rgbaStart + 1] = Clip(gVal);
        rgbaArrayBuffer[rgbaStart + 2] = Clip(bVal);
        rgbaArrayBuffer[rgbaStart + 3] = Clip(aVal);
    }

    private byte Clip(double x)
    {
        return (byte)(x > 255 ? 255 : x < 0 ? 0 : x);
    }

    public void Start()
    {
        Log.d(TAG, "Start");

        backgroundHandlerThread = new HandlerThread("Camera Thread");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());

        OpenCamera();
    }

    public void Stop()
    {
        Log.d(TAG, "Stop");
        CloseCamera();

        try
        {
            backgroundHandlerThread.quitSafely();
            backgroundHandlerThread.join();
            backgroundHandlerThread = null;
            backgroundHandler = null;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @SuppressLint("WrongConstant")
    private void AttachSurface()
    {
        imageReader = ImageReader.newInstance(w, h, ImageFormat.YUV_420_888, 1);
        imageReader.setOnImageAvailableListener(this, backgroundHandler);
        surface = imageReader.getSurface();

        try
        {
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback()
            {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession _cameraCaptureSession)
                {
                    cameraCaptureSession = _cameraCaptureSession;

                    UpdateSurface();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession _cameraCaptureSession)
                {
                    _cameraCaptureSession.close();
                }

                @Override
                public void onClosed(CameraCaptureSession session)
                {

                }
            }, backgroundHandler);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void UpdateSurface()
    {
        try
        {
            CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            captureRequestBuilder.addTarget(surface);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static List<String> GetCameraList(Context context)
    {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        List<String> cameraList = new ArrayList<>();

        try
        {
            String[] cameras = cameraManager.getCameraIdList();
            cameraList = Arrays.asList(cameras);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return cameraList;
    }
}