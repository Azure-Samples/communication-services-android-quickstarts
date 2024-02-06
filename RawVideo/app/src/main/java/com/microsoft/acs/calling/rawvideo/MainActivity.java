package com.microsoft.acs.calling.rawvideo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.azure.android.communication.calling.Call;
import com.azure.android.communication.calling.CallAgent;
import com.azure.android.communication.calling.CallAgentOptions;
import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.CallVideoStream;
import com.azure.android.communication.calling.CallingCommunicationException;
import com.azure.android.communication.calling.CreateViewOptions;
import com.azure.android.communication.calling.DeviceManager;
import com.azure.android.communication.calling.HangUpOptions;
import com.azure.android.communication.calling.IncomingVideoOptions;
import com.azure.android.communication.calling.IncomingVideoStream;
import com.azure.android.communication.calling.JoinCallOptions;
import com.azure.android.communication.calling.JoinMeetingLocator;
import com.azure.android.communication.calling.LocalVideoStream;
import com.azure.android.communication.calling.OutgoingVideoOptions;
import com.azure.android.communication.calling.OutgoingVideoStream;
import com.azure.android.communication.calling.ParticipantsUpdatedEvent;
import com.azure.android.communication.calling.RawIncomingVideoStream;
import com.azure.android.communication.calling.RawOutgoingVideoStreamOptions;
import com.azure.android.communication.calling.RawVideoFrameBuffer;
import com.azure.android.communication.calling.RawVideoFrameReceivedEvent;
import com.azure.android.communication.calling.RemoteParticipant;
import com.azure.android.communication.calling.RemoteVideoStream;
import com.azure.android.communication.calling.ScalingMode;
import com.azure.android.communication.calling.ScreenShareOutgoingVideoStream;
import com.azure.android.communication.calling.TeamsMeetingLinkLocator;
import com.azure.android.communication.calling.VideoDeviceInfo;
import com.azure.android.communication.calling.VideoStreamFormat;
import com.azure.android.communication.calling.VideoStreamPixelFormat;
import com.azure.android.communication.calling.VideoStreamRenderer;
import com.azure.android.communication.calling.VideoStreamRendererView;
import com.azure.android.communication.calling.VideoStreamResolution;
import com.azure.android.communication.calling.VideoStreamStateChangedEvent;
import com.azure.android.communication.calling.VideoStreamType;
import com.azure.android.communication.calling.VirtualOutgoingVideoStream;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.microsoft.acs.calling.rawvideo.ui.CameraListViewAdapter;
import com.microsoft.acs.calling.rawvideo.ui.VideoDeviceListViewAdapter;
import com.microsoft.acs.calling.rawvideo.ui.VideoStreamTypeListViewAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
{
    // UI
    private LinearLayout settingsContainer;
    private ConstraintLayout videoContainer;
    private EditText tokenEditText;
    private EditText meetingUrlEditText;
    private Spinner videoDeviceInfoSpinner;
    private Spinner cameraSpinner;
    private LinearLayout outgoingVideoContainer;
    private LinearLayout incomingVideoContainer;
    private int selectVideoDeviceInfoIndex = -1;
    private int selectCameraIndex = -1;

    // App
    private Map<Integer, IncomingVideoStream> incomingVideoStreamMap;
    private List<VideoStreamType> outgoingVideoStreamTypeList;
    private List<VideoStreamType> incomingVideoStreamTypeList;
    private List<VideoDeviceInfo> videoDeviceInfoList;
    private List<String> cameraList;
    private CallClient callClient;
    private CallAgent callAgent;
    private Call call;
    private DeviceManager deviceManager;
    private ScreenCaptureService screenCaptureService;
    private CameraCaptureService cameraCaptureService;
    private OutgoingVideoStream outgoingVideoStream;
    private LocalVideoStream localVideoStream;
    private VirtualOutgoingVideoStream virtualOutgoingVideoStream;
    private ScreenShareOutgoingVideoStream screenShareOutgoingVideoStream;
    private VideoStreamRenderer outgoingVideoStreamRenderer;
    private VideoStreamRendererView outgoingVideoStreamRendererView;
    private VideoStreamRenderer incomingVideoStreamRenderer;
    private VideoStreamRendererView incomingVideoStreamRendererView;
    private VideoFrameRenderer incomingVideoFrameRenderer;
    private VideoFrameRenderer outgoingVideoFrameRenderer;
    private VideoStreamType outgoingVideoStreamType;
    private VideoStreamType incomingVideoStreamType;
    private Intent screenShareServiceIntent;
    private int w = 0;
    private int h = 0;
    private int frameRate = 30;
    private double maxWidth = 1920.0;
    private double maxHeight = 1080.0;
    private boolean callInProgress = false;
    private boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FindUIVariables();
        InitializeTestCase();
        GetPermissions();

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    private void FindUIVariables()
    {
        outgoingVideoContainer = findViewById(R.id.outgoingVideoContainer);
        incomingVideoContainer = findViewById(R.id.incomingVideoContainer);

        tokenEditText = findViewById(R.id.TokenEditText);
        meetingUrlEditText = findViewById(R.id.MeetingUrlEditText);
    }

    @SuppressLint("SetTextI18n")
    private void InitializeTestCase()
    {
        incomingVideoStreamMap = new HashMap<>();

        settingsContainer = findViewById(R.id.settings_container);
        videoContainer = findViewById(R.id.videoContainer);

        CreateCallAgent();

        incomingVideoStreamTypeList = Arrays.asList(VideoStreamType.REMOTE_INCOMING,
                VideoStreamType.RAW_INCOMING);
        VideoStreamTypeListViewAdapter incomingVideoDeviceInfoListViewAdapter =
                new VideoStreamTypeListViewAdapter(this, incomingVideoStreamTypeList);

        Spinner incomingVideoStreamTypeSpinner = findViewById(R.id.incomingVideoStreamTypeSpinner);
        incomingVideoStreamTypeSpinner.setAdapter(incomingVideoDeviceInfoListViewAdapter);
        incomingVideoStreamTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long id)
            {
                incomingVideoStreamType = incomingVideoStreamTypeList.get(index);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
        incomingVideoStreamTypeSpinner.setSelection(1);

        outgoingVideoStreamTypeList = Arrays.asList(VideoStreamType.LOCAL_OUTGOING,
                VideoStreamType.VIRTUAL_OUTGOING,
                VideoStreamType.SCREEN_SHARE_OUTGOING);
        VideoStreamTypeListViewAdapter outgoingVideoDeviceInfoListViewAdapter =
                new VideoStreamTypeListViewAdapter(this, outgoingVideoStreamTypeList);

        Spinner outgoingVideoStreamTypeSpinner = findViewById(R.id.outgoingVideoStreamTypeSpinner);
        outgoingVideoStreamTypeSpinner.setAdapter(outgoingVideoDeviceInfoListViewAdapter);
        outgoingVideoStreamTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long id)
            {
                outgoingVideoStreamType = outgoingVideoStreamTypeList.get(index);
                switch (outgoingVideoStreamType)
                {
                    case LOCAL_OUTGOING:
                        videoDeviceInfoSpinner.setVisibility(View.VISIBLE);
                        cameraSpinner.setVisibility(View.GONE);
                        break;
                    case VIRTUAL_OUTGOING:
                        cameraSpinner.setVisibility(View.VISIBLE);
                        videoDeviceInfoSpinner.setVisibility(View.GONE);
                        break;
                    case SCREEN_SHARE_OUTGOING:
                        videoDeviceInfoSpinner.setVisibility(View.GONE);
                        cameraSpinner.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
        outgoingVideoStreamTypeSpinner.setSelection(1);

        videoDeviceInfoList = deviceManager.getCameras();
        VideoDeviceListViewAdapter videoDeviceInfoListViewAdapter =
                new VideoDeviceListViewAdapter(this, videoDeviceInfoList);

        videoDeviceInfoSpinner = findViewById(R.id.videoDeviceInfoSpinner);
        videoDeviceInfoSpinner.setAdapter(videoDeviceInfoListViewAdapter);
        videoDeviceInfoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long id)
            {
                selectVideoDeviceInfoIndex = index;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        cameraList = CameraCaptureService.GetCameraList(this);
        CameraListViewAdapter cameraListViewAdapter = new CameraListViewAdapter(this, cameraList);

        cameraSpinner = findViewById(R.id.cameraSpinner);;
        cameraSpinner.setAdapter(cameraListViewAdapter);
        cameraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long id)
            {
                selectCameraIndex = index;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
    }

    private void GetPermissions()
    {
        String[] requiredPermissions = new String[]
        {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE
        };

        ArrayList<String> permissionsToAskFor = new ArrayList<>();
        for (String permission : requiredPermissions)
        {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
            {
                permissionsToAskFor.add(permission);
            }
        }

        if (!permissionsToAskFor.isEmpty())
        {
            ActivityCompat.requestPermissions(this, permissionsToAskFor.toArray(new String[0]), 1);
        }
    }

    public void GetScreenSharePermissions()
    {
        if (screenShareServiceIntent == null)
        {
            try
            {
                MediaProjectionManager mediaProjectionManager =
                        (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                        Constants.SCREEN_SHARE_REQUEST_INTENT_REQ_CODE);
            }
            catch (Exception ex)
            {
                EndCall(null);

                ShowMessage("Screen share failed to start");
            }
        }
        else
        {
            StartScreenShareCaptureService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.SCREEN_SHARE_REQUEST_INTENT_REQ_CODE)
        {
            if (resultCode == Activity.RESULT_OK && data != null)
            {
                screenShareServiceIntent = data;
                StartScreenShareCaptureService();
            }
            else
            {
                Toast.makeText(getApplicationContext(),
                                "User cancelled, did not give permission to capture screen",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void CreateCallAgent()
    {
        String token = tokenEditText.getText().toString();
        if (token.isEmpty())
        {
            Toast.makeText(getApplicationContext(),
                            "Token is not valid",
                            Toast.LENGTH_SHORT)
                    .show();

            return;
        }

        try
        {
            CommunicationTokenCredential credential = new CommunicationTokenCredential(token);
            callClient = new CallClient();

            CallAgentOptions callAgentOptions = new CallAgentOptions();
            callAgentOptions.setDisplayName("Android Quickstart User");

            callAgent = callClient.createCallAgent(getApplicationContext(), credential, callAgentOptions).get();

            deviceManager = callClient.getDeviceManager(this).get();
        }
        catch (Exception ex)
        {
            Toast.makeText(getApplicationContext(),
                    "Failed to create call agent",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void StartCall(View view)
    {
        if (callInProgress)
        {
            return;
        }

        if (!ValidateCallSettings())
        {
            return;
        }

        callInProgress = true;

        IncomingVideoOptions incomingVideoOptions = new IncomingVideoOptions()
                .setStreamType(incomingVideoStreamType);

        OutgoingVideoOptions outgoingVideoOptions = CreateOutgoingVideoOptions();

        JoinCallOptions joinCallOptions = new JoinCallOptions()
                .setIncomingVideoOptions(incomingVideoOptions)
                .setOutgoingVideoOptions(outgoingVideoOptions);

        String meetingLink = meetingUrlEditText.getText().toString();
        JoinMeetingLocator locator = new TeamsMeetingLinkLocator(meetingLink);

        try
        {
            call = callAgent.join(getApplicationContext(), locator, joinCallOptions);

            callInProgress = true;
            settingsContainer.setVisibility(View.GONE);
            videoContainer.setVisibility(View.VISIBLE);
        }
        catch (CallingCommunicationException ex)
        {
            callInProgress = false;

            ShowMessage("Call failed to start");
        }

        if (call != null)
        {
            call.addOnRemoteParticipantsUpdatedListener(this::OnRemoteParticipantsUpdated);

            AddRemoteParticipantList(call.getRemoteParticipants());
        }
    }

    private void OnRemoteParticipantsUpdated(ParticipantsUpdatedEvent event)
    {
        AddRemoteParticipantList(event.getAddedParticipants());

        for (RemoteParticipant remoteParticipant : event.getRemovedParticipants())
        {
            remoteParticipant.removeOnVideoStreamStateChangedListener(this::OnVideoStreamStateChanged);
        }
    }

    private void AddRemoteParticipantList(List<RemoteParticipant> remoteParticipantList)
    {
        for (RemoteParticipant remoteParticipant : remoteParticipantList)
        {
            remoteParticipant.addOnVideoStreamStateChangedListener(this::OnVideoStreamStateChanged);
            for (IncomingVideoStream incomingVideoStream : remoteParticipant.getIncomingVideoStreams())
            {
                OnIncomingVideoStreamStateChanged(incomingVideoStream);
            }
        }
    }

    private OutgoingVideoOptions CreateOutgoingVideoOptions()
    {
        RawOutgoingVideoStreamOptions options = CreateRawOutgoingVideoStreamOptions();
        switch (outgoingVideoStreamType)
        {
            case LOCAL_OUTGOING:
                localVideoStream =
                        new LocalVideoStream(videoDeviceInfoList.get(selectVideoDeviceInfoIndex), this);
                localVideoStream.addOnStateChangedListener(this::OnVideoStreamStateChanged);
                outgoingVideoStream = localVideoStream;

                break;
            case VIRTUAL_OUTGOING:
                virtualOutgoingVideoStream =
                        new VirtualOutgoingVideoStream(options);
                virtualOutgoingVideoStream.addOnStateChangedListener(this::OnVideoStreamStateChanged);
                outgoingVideoStream = virtualOutgoingVideoStream;

                break;
            case SCREEN_SHARE_OUTGOING:
                screenShareOutgoingVideoStream =
                        new ScreenShareOutgoingVideoStream(options);
                screenShareOutgoingVideoStream.addOnStateChangedListener(this::OnVideoStreamStateChanged);
                outgoingVideoStream = screenShareOutgoingVideoStream;

                break;
        }

        return new OutgoingVideoOptions()
                .setOutgoingVideoStreams(Arrays.asList(outgoingVideoStream));
    }

    private RawOutgoingVideoStreamOptions CreateRawOutgoingVideoStreamOptions()
    {
        VideoStreamFormat format = CreateVideoStreamFormat();

        RawOutgoingVideoStreamOptions options = new RawOutgoingVideoStreamOptions();
        options.setFormats(Arrays.asList(format));

        return options;
    }

    private VideoStreamFormat CreateVideoStreamFormat()
    {
        VideoStreamFormat format = new VideoStreamFormat();
        format.setPixelFormat(VideoStreamPixelFormat.RGBA);
        format.setFramesPerSecond(frameRate);

        switch (outgoingVideoStreamType)
        {
            case VIRTUAL_OUTGOING:
                format.setResolution(VideoStreamResolution.P360);
                w = format.getWidth();
                h = format.getHeight();
                break;
            case SCREEN_SHARE_OUTGOING:
                GetDisplaySize();
                format.setWidth(w);
                format.setHeight(h);
                break;
        }

        format.setStride1(w * 4);

        return format;
    }

    private void OnVideoStreamStateChanged(VideoStreamStateChangedEvent event)
    {
        CallVideoStream stream = event.getStream();
        switch (stream.getDirection())
        {
            case OUTGOING:
                OnOutgoingVideoStreamStateChanged((OutgoingVideoStream) stream);
                break;
            case INCOMING:
                OnIncomingVideoStreamStateChanged((IncomingVideoStream) stream);
                break;
        }
    }

    private void OnOutgoingVideoStreamStateChanged(OutgoingVideoStream stream)
    {
        switch (stream.getState())
        {
            case AVAILABLE:
                if (stream.getType() == VideoStreamType.LOCAL_OUTGOING)
                {
                    StartLocalPreview();
                }

                break;
            case STARTED:
                switch (stream.getType())
                {
                    case VIRTUAL_OUTGOING:
                        StartCameraCaptureService();
                        break;
                    case SCREEN_SHARE_OUTGOING:
                        GetScreenSharePermissions();
                        break;
                }

                break;
            case STOPPED:
                switch (stream.getType())
                {
                    case LOCAL_OUTGOING:
                        StopLocalPreview();
                        break;
                    case VIRTUAL_OUTGOING:
                        StopCameraCaptureService();
                        break;
                    case SCREEN_SHARE_OUTGOING:
                        StopScreenShareCaptureService();
                        break;
                }

                break;
        }
    }

    private void OnIncomingVideoStreamStateChanged(IncomingVideoStream stream)
    {
        switch (stream.getState())
        {
            case AVAILABLE:
                if (!incomingVideoStreamMap.containsKey(stream.getId()))
                {
                    switch (stream.getType())
                    {
                        case REMOTE_INCOMING:
                            StartRemotePreview((RemoteVideoStream) stream);
                            break;
                        case RAW_INCOMING:
                            RawIncomingVideoStream rawIncomingVideoStream = (RawIncomingVideoStream) stream;
                            rawIncomingVideoStream.addOnRawVideoFrameReceivedListener(this::OnRawVideoFrameReceived);
                            rawIncomingVideoStream.start();

                            break;
                    }

                    incomingVideoStreamMap.put(stream.getId(), stream);
                }

                break;
            case STARTED:
                if (incomingVideoStreamMap.containsKey(stream.getId()))
                {
                    if (stream.getType() == VideoStreamType.RAW_INCOMING)
                    {
                        StartRawIncomingPreview();
                    }
                }
                break;
            case STOPPED:
                if (incomingVideoStreamMap.containsKey(stream.getId()))
                {
                    switch (stream.getType())
                    {
                        case REMOTE_INCOMING:
                            StopRemotePreview();
                            break;
                        case RAW_INCOMING:
                            StopRawIncomingPreview();
                            break;
                    }
                }

                break;
            case NOT_AVAILABLE:
                if (incomingVideoStreamMap.containsKey(stream.getId()))
                {
                    if (stream.getType() == VideoStreamType.RAW_INCOMING)
                    {
                        RawIncomingVideoStream rawIncomingVideoStream =
                                (RawIncomingVideoStream) incomingVideoStreamMap.get(stream.getId());
                        rawIncomingVideoStream.removeOnRawVideoFrameReceivedListener(this::OnRawVideoFrameReceived);
                    }

                    incomingVideoStreamMap.remove(stream.getId());
                }

                break;
        }
    }

    private void OnRawVideoFrameReceived(RawVideoFrameReceivedEvent event)
    {
        RawVideoFrameBuffer rawVideoFrameBuffer = (RawVideoFrameBuffer) event.getFrame();
        try
        {
            incomingVideoFrameRenderer.RenderRawVideoFrame(rawVideoFrameBuffer);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            rawVideoFrameBuffer.close();
        }
    }

    public void OnRawVideoFrameCaptured(RawVideoFrameBuffer rawVideoFrameBuffer, int orientation)
    {
        try
        {
            outgoingVideoFrameRenderer.RenderRawVideoFrame(rawVideoFrameBuffer, orientation);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            rawVideoFrameBuffer.close();
        }
    }

    private void StartRemotePreview(RemoteVideoStream remoteVideoStream)
    {
        if (incomingVideoStreamRendererView == null)
        {
            incomingVideoStreamRenderer = new VideoStreamRenderer(remoteVideoStream, this);
            incomingVideoStreamRendererView = incomingVideoStreamRenderer.createView(new CreateViewOptions(ScalingMode.FIT));
            runOnUiThread(() -> incomingVideoContainer.addView(incomingVideoStreamRendererView, 0));
        }
    }

    private void StopRemotePreview()
    {
        if (incomingVideoStreamRendererView != null)
        {
            runOnUiThread(() -> incomingVideoContainer.removeViewAt(0));

            incomingVideoStreamRendererView.dispose();
            incomingVideoStreamRendererView = null;

            incomingVideoStreamRenderer.dispose();
            incomingVideoStreamRenderer = null;
        }
    }

    private void StartRawIncomingPreview()
    {
        if (incomingVideoFrameRenderer == null)
        {
            incomingVideoFrameRenderer =
                    new VideoFrameRenderer(this, 320, 180, ScalingMode.FIT, false, false);
            runOnUiThread(() -> incomingVideoContainer.addView(incomingVideoFrameRenderer.GetView()));
        }
    }

    private void StopRawIncomingPreview()
    {
        if (incomingVideoFrameRenderer != null)
        {
            runOnUiThread(() -> incomingVideoContainer.removeViewAt(0));
            incomingVideoFrameRenderer.Dispose();
            incomingVideoFrameRenderer = null;
        }
    }

    private void StartLocalPreview()
    {
        if (outgoingVideoStreamRendererView == null)
        {
            outgoingVideoStreamRenderer = new VideoStreamRenderer(localVideoStream, this);
            outgoingVideoStreamRendererView = outgoingVideoStreamRenderer.createView(new CreateViewOptions(ScalingMode.FIT));
            runOnUiThread(() -> outgoingVideoContainer.addView(outgoingVideoStreamRendererView, 0));
        }
    }

    private void StopLocalPreview()
    {
        if (outgoingVideoStreamRendererView != null)
        {
            runOnUiThread(() -> outgoingVideoContainer.removeViewAt(0));

            outgoingVideoStreamRendererView.dispose();
            outgoingVideoStreamRendererView = null;

            outgoingVideoStreamRenderer.dispose();
            outgoingVideoStreamRenderer = null;
        }
    }

    private void StartCameraCaptureService()
    {
        if (cameraCaptureService == null)
        {
            cameraCaptureService = new CameraCaptureService(this,
                    virtualOutgoingVideoStream,
                    cameraList.get(selectCameraIndex),
                    w,
                    h);
            cameraCaptureService.AddRawVideoFrameListener(this::OnRawVideoFrameCaptured);
            cameraCaptureService.Start();

            outgoingVideoFrameRenderer =
                    new VideoFrameRenderer(this, 120, 67, ScalingMode.FIT, true, true);
            runOnUiThread(() -> outgoingVideoContainer.addView(outgoingVideoFrameRenderer.GetView()));
        }
    }

    private void StopCameraCaptureService()
    {
        if (cameraCaptureService != null)
        {
            runOnUiThread(() -> outgoingVideoContainer.removeViewAt(0));
            outgoingVideoFrameRenderer.Dispose();
            outgoingVideoFrameRenderer = null;

            cameraCaptureService.RemoveRawVideoFrameListener(this::OnRawVideoFrameCaptured);
            cameraCaptureService.Stop();
            cameraCaptureService = null;
        }
    }

    private void StartScreenShareCaptureService()
    {
        if (screenCaptureService == null)
        {
            screenCaptureService = new ScreenCaptureService(this,
                    screenShareOutgoingVideoStream,
                    w,
                    h,
                    frameRate,
                    Activity.RESULT_OK,
                    screenShareServiceIntent);
            screenCaptureService.AddRawVideoFrameListener(this::OnRawVideoFrameCaptured);
            screenCaptureService.Start();

            outgoingVideoFrameRenderer =
                    new VideoFrameRenderer(this, 120, 67, ScalingMode.FIT, false, true);
            runOnUiThread(() -> outgoingVideoContainer.addView(outgoingVideoFrameRenderer.GetView()));
        }
    }

    private void StopScreenShareCaptureService()
    {
        if (screenCaptureService != null)
        {
            runOnUiThread(() -> outgoingVideoContainer.removeViewAt(0));
            outgoingVideoFrameRenderer.Dispose();
            outgoingVideoFrameRenderer = null;

            screenCaptureService.RemoveRawVideoFrameListener(this::OnRawVideoFrameCaptured);
            screenCaptureService.Stop();
            screenCaptureService = null;
        }
    }

    public void EndCall(View view)
    {
        if (!callInProgress)
        {
            return;
        }

        Executors.newCachedThreadPool().submit(() -> {
            try
            {
                if (call != null)
                {
                    call.removeOnRemoteParticipantsUpdatedListener(this::OnRemoteParticipantsUpdated);

                    StopRemotePreview();
                    StopRawIncomingPreview();

                    StopCameraCaptureService();
                    StopScreenShareCaptureService();

                    if (virtualOutgoingVideoStream != null)
                    {
                        virtualOutgoingVideoStream.removeOnStateChangedListener(this::OnVideoStreamStateChanged);
                        virtualOutgoingVideoStream = null;
                    }

                    if (screenShareOutgoingVideoStream != null)
                    {
                        screenShareOutgoingVideoStream.removeOnStateChangedListener(this::OnVideoStreamStateChanged);
                        screenShareOutgoingVideoStream = null;
                    }

                    if (localVideoStream != null)
                    {
                        localVideoStream.removeOnStateChangedListener(this::OnVideoStreamStateChanged);
                        localVideoStream = null;
                    }

                    if (outgoingVideoStream != null)
                    {
                        call.stopVideo(this, outgoingVideoStream).get();
                        outgoingVideoStream = null;
                    }

                    call.hangUp(new HangUpOptions()).get();
                    call = null;
                }

                incomingVideoStreamMap.clear();

                callInProgress = false;
                runOnUiThread(() -> {
                    settingsContainer.setVisibility(View.VISIBLE);
                    videoContainer.setVisibility(View.GONE);
                });
            }
            catch (Exception ex)
            {
                ShowMessage("Call failed to stop");
            }
        });
    }

    private void GetDisplaySize()
    {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        w = displayMetrics.widthPixels;
        h = displayMetrics.heightPixels;

        if (h > maxHeight)
        {
            double percentage = Math.abs((maxHeight / h) - 1);
            w = (int) Math.ceil((w * percentage));
            h = (int) maxHeight;
        }

        if (w > maxWidth)
        {
            double percentage = Math.abs((maxWidth / w) - 1);
            h = (int) Math.ceil((h * percentage));
            w = (int) maxWidth;
        }
    }

    private boolean ValidateCallSettings()
    {
        boolean isValid = true;
        switch (outgoingVideoStreamType)
        {
            case LOCAL_OUTGOING:
                isValid = selectVideoDeviceInfoIndex != -1;
                break;
            case VIRTUAL_OUTGOING:
                isValid = selectCameraIndex != -1;
                break;
        }

        return isValid;
    }

    private void ShowMessage(String message)
    {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(),
                                message,
                                Toast.LENGTH_LONG)
                        .show());
    }
}