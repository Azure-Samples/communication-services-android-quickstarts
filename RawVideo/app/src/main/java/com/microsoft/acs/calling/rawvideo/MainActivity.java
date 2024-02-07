package com.microsoft.acs.calling.rawvideo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.azure.android.communication.calling.Call;
import com.azure.android.communication.calling.CallAgent;
import com.azure.android.communication.calling.CallAgentOptions;
import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.CallVideoStream;
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
import com.azure.android.communication.calling.VideoStreamState;
import com.azure.android.communication.calling.VideoStreamStateChangedEvent;
import com.azure.android.communication.calling.VideoStreamType;
import com.azure.android.communication.calling.VirtualOutgoingVideoStream;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.microsoft.acs.calling.rawvideo.ui.CameraListViewAdapter;
import com.microsoft.acs.calling.rawvideo.ui.VideoDeviceListViewAdapter;
import com.microsoft.acs.calling.rawvideo.ui.VideoStreamTypeListViewAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
{
    // UI
    private LinearLayout callSettingsContainer;
    private ConstraintLayout videoContainer;
    private EditText tokenEditText;
    private EditText meetingLinkEditText;
    private Spinner videoDeviceInfoSpinner;
    private Spinner cameraSpinner;
    private LinearLayout outgoingVideoContainer;
    private LinearLayout incomingVideoContainer;
    private ProgressDialog progressDialog;
    private int selectVideoDeviceInfoIndex = -1;
    private int selectCameraIndex = -1;

    // App
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
    private IncomingVideoStream incomingVideoStream;
    private RemoteVideoStream remoteVideoStream;
    private RawIncomingVideoStream rawIncomingVideoStream;
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
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;
    private int w = 0;
    private int h = 0;
    private int frameRate = 30;
    private double maxWidth = 1920.0;
    private double maxHeight = 1080.0;
    private boolean callInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPreferences.edit();

        String savedToken = sharedPreferences.getString("Token", null);
        tokenEditText = findViewById(R.id.token_edit_text);

        if (savedToken != null)
        {
            tokenEditText.setText(savedToken);
        }

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @SuppressLint("SetTextI18n")
    private void InitResources()
    {
        videoContainer = findViewById(R.id.videoContainer);
        outgoingVideoContainer = findViewById(R.id.outgoing_video_container);
        incomingVideoContainer = findViewById(R.id.incoming_video_container);
        callSettingsContainer = findViewById(R.id.call_settings_container);

        CreateCallAgent();

        if (deviceManager == null)
        {
            return;
        }

        incomingVideoStreamTypeList = Arrays.asList(VideoStreamType.REMOTE_INCOMING,
                VideoStreamType.RAW_INCOMING);
        VideoStreamTypeListViewAdapter incomingVideoDeviceInfoListViewAdapter =
                new VideoStreamTypeListViewAdapter(this, incomingVideoStreamTypeList);

        Spinner incomingVideoStreamTypeSpinner = findViewById(R.id.incoming_video_stream_type_spinner);
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

        Spinner outgoingVideoStreamTypeSpinner = findViewById(R.id.outgoing_video_stream_type_spinner);
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

        videoDeviceInfoSpinner = findViewById(R.id.video_device_info_spinner);
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

        cameraSpinner = findViewById(R.id.camera_spinner);;
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

        LinearLayout tokenContainer = findViewById(R.id.token_container);
        tokenContainer.setVisibility(View.GONE);

        LinearLayout callContainer = findViewById(R.id.call_container);
        callContainer.setVisibility(View.VISIBLE);

        String savedMeetingLink = sharedPreferences.getString("MeetingLink", null);
        meetingLinkEditText = findViewById(R.id.meeting_link_edit_text);

        if (savedMeetingLink != null)
        {
            meetingLinkEditText.setText(savedMeetingLink);
        }
    }

    public void GetPermissions(View view)
    {
        if (tokenEditText.getText().toString().isEmpty())
        {
            ShowMessage("Invalid token");
            return;
        }

        String[] requiredPermissions = new String[]
        {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
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
        else
        {
            InitResources();
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
                ShowMessage("Screen capture service failed to start");

                EndCall(null);
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
                ShowMessage("Permission denied for capture the screen");
            }
        }
    }

    private void CreateCallAgent()
    {
        try
        {
            CommunicationTokenCredential credential =
                    new CommunicationTokenCredential(tokenEditText.getText().toString());
            callClient = new CallClient();

            CallAgentOptions callAgentOptions = new CallAgentOptions();
            callAgentOptions.setDisplayName("Android Quickstart User");

            callAgent = callClient.createCallAgent(getApplicationContext(), credential, callAgentOptions).get();

            deviceManager = callClient.getDeviceManager(this).get();

            editor.putString("Token", tokenEditText.getText().toString());
            editor.apply();
        }
        catch (Exception ex)
        {
            ShowMessage("Failed to create call agent");
            ex.printStackTrace();
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

        runOnUiThread(() -> progressDialog.show());
        Executors.newCachedThreadPool().submit(() ->
        {
            IncomingVideoOptions incomingVideoOptions = new IncomingVideoOptions()
                    .setStreamType(incomingVideoStreamType);

            OutgoingVideoOptions outgoingVideoOptions = CreateOutgoingVideoOptions();

            JoinCallOptions joinCallOptions = new JoinCallOptions()
                    .setIncomingVideoOptions(incomingVideoOptions)
                    .setOutgoingVideoOptions(outgoingVideoOptions);

            JoinMeetingLocator locator =
                    new TeamsMeetingLinkLocator(meetingLinkEditText.getText().toString());

            try
            {
                call = callAgent.join(getApplicationContext(), locator, joinCallOptions);

                call.muteOutgoingAudio(this).get();
                call.muteIncomingAudio(this).get();

                runOnUiThread(() ->
                {
                    callSettingsContainer.setVisibility(View.GONE);
                    videoContainer.setVisibility(View.VISIBLE);
                });

                editor.putString("MeetingLink", meetingLinkEditText.getText().toString());
                editor.apply();
            }
            catch (Exception ex)
            {
                callInProgress = false;

                ShowMessage("Call failed to start");
                ex.printStackTrace();
            }

            if (call != null)
            {
                call.addOnRemoteParticipantsUpdatedListener(this::OnRemoteParticipantsUpdated);

                AddRemoteParticipantList(call.getRemoteParticipants());
            }

            runOnUiThread(() -> progressDialog.hide());
        });
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

                outgoingVideoStream = null;
                break;
        }
    }

    private void OnIncomingVideoStreamStateChanged(IncomingVideoStream stream)
    {
        if (incomingVideoStream != null && incomingVideoStream != stream)
        {
            if (stream.getState() == VideoStreamState.AVAILABLE)
            {
                ShowMessage("This app only support 1 incoming video stream from 1 remote participant");
            }

            return;
        }

        switch (stream.getState())
        {
            case AVAILABLE:
                switch (stream.getType())
                {
                    case REMOTE_INCOMING:
                        remoteVideoStream = (RemoteVideoStream) stream;
                        StartRemotePreview();
                        break;
                    case RAW_INCOMING:
                        rawIncomingVideoStream = (RawIncomingVideoStream) stream;
                        rawIncomingVideoStream.addOnRawVideoFrameReceivedListener(this::OnRawVideoFrameReceived);
                        rawIncomingVideoStream.start();

                        break;
                }

                incomingVideoStream = stream;
                break;
            case STARTED:
                if (stream.getType() == VideoStreamType.RAW_INCOMING)
                {
                    StartRawIncomingPreview();
                }

                break;
            case STOPPED:
                switch (stream.getType())
                {
                    case REMOTE_INCOMING:
                        StopRemotePreview();
                        break;
                    case RAW_INCOMING:
                        StopRawIncomingPreview();
                        break;
                }

                break;
            case NOT_AVAILABLE:
                if (stream.getType() == VideoStreamType.RAW_INCOMING)
                {
                    rawIncomingVideoStream.removeOnRawVideoFrameReceivedListener(this::OnRawVideoFrameReceived);
                }

                incomingVideoStream = null;
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

    private void StartRemotePreview()
    {
        if (incomingVideoStreamRendererView == null)
        {
            incomingVideoStreamRenderer = new VideoStreamRenderer(remoteVideoStream, this);
            incomingVideoStreamRendererView = incomingVideoStreamRenderer.createView(new CreateViewOptions(ScalingMode.FIT));

            AddVideoView(incomingVideoContainer, incomingVideoStreamRendererView);
        }
    }

    private void StopRemotePreview()
    {
        if (incomingVideoStreamRendererView != null)
        {
            RemoveVideoView(incomingVideoContainer);

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
            AddVideoView(incomingVideoContainer, incomingVideoFrameRenderer.GetView());
        }
    }

    private void StopRawIncomingPreview()
    {
        if (incomingVideoFrameRenderer != null)
        {
            runOnUiThread(() ->
            {
                RemoveVideoView(incomingVideoContainer);
                incomingVideoFrameRenderer.ClearView();
                incomingVideoFrameRenderer = null;
            });
        }
    }

    private void StartLocalPreview()
    {
        if (outgoingVideoStreamRendererView == null)
        {
            outgoingVideoStreamRenderer = new VideoStreamRenderer(localVideoStream, this);
            outgoingVideoStreamRendererView = outgoingVideoStreamRenderer.createView(new CreateViewOptions(ScalingMode.FIT));
            AddVideoView(outgoingVideoContainer, outgoingVideoStreamRendererView);
        }
    }

    private void StopLocalPreview()
    {
        if (outgoingVideoStreamRendererView != null)
        {
            RemoveVideoView(outgoingVideoContainer);

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
            AddVideoView(outgoingVideoContainer, outgoingVideoFrameRenderer.GetView());
        }
    }

    private void StopCameraCaptureService()
    {
        if (cameraCaptureService != null)
        {
            runOnUiThread(() ->
            {
                RemoveVideoView(outgoingVideoContainer);
                outgoingVideoFrameRenderer.ClearView();
                outgoingVideoFrameRenderer = null;
            });

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
            AddVideoView(outgoingVideoContainer, outgoingVideoFrameRenderer.GetView());
        }
    }

    private void StopScreenShareCaptureService()
    {
        if (screenCaptureService != null)
        {
            runOnUiThread(() ->
            {
                RemoveVideoView(outgoingVideoContainer);
                outgoingVideoFrameRenderer.ClearView();
                outgoingVideoFrameRenderer = null;
            });

            screenCaptureService.RemoveRawVideoFrameListener(this::OnRawVideoFrameCaptured);
            screenCaptureService.Stop();
            screenCaptureService = null;
        }
    }

    private void AddVideoView(ViewGroup videoContainer, View videoView)
    {
        runOnUiThread(() ->
        {
            videoContainer.setBackgroundResource(R.drawable.acs_spool_ui_black_border_black_background);
            videoContainer.addView(videoView);
        });
    }

    private void RemoveVideoView(ViewGroup videoContainer)
    {
        runOnUiThread(() ->
        {
            videoContainer.removeViewAt(0);
            videoContainer.setBackgroundResource(R.drawable.acs_spool_ui_black_border);
        });
    }

    public void EndCall(View view)
    {
        if (!callInProgress)
        {
            return;
        }

        runOnUiThread(() -> progressDialog.show());
        Executors.newCachedThreadPool().submit(() ->
        {
            try
            {
                if (call != null)
                {
                    for (RemoteParticipant remoteParticipant : call.getRemoteParticipants())
                    {
                        remoteParticipant.removeOnVideoStreamStateChangedListener(this::OnVideoStreamStateChanged);
                    }

                    call.removeOnRemoteParticipantsUpdatedListener(this::OnRemoteParticipantsUpdated);

                    StopRemotePreview();
                    StopRawIncomingPreview();

                    incomingVideoStream = null;
                    remoteVideoStream = null;
                    rawIncomingVideoStream = null;

                    StopLocalPreview();
                    StopCameraCaptureService();
                    StopScreenShareCaptureService();

                    if (localVideoStream != null)
                    {
                        localVideoStream.removeOnStateChangedListener(this::OnVideoStreamStateChanged);
                        localVideoStream = null;
                    }

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

                    if (outgoingVideoStream != null)
                    {
                        call.stopVideo(this, outgoingVideoStream).get();
                        outgoingVideoStream = null;
                    }

                    call.hangUp(new HangUpOptions()).get();
                    call = null;
                }

                callInProgress = false;

                runOnUiThread(() ->
                {
                    callSettingsContainer.setVisibility(View.VISIBLE);
                    videoContainer.setVisibility(View.GONE);
                });
            }
            catch (Exception ex)
            {
                ShowMessage("Call failed to stop");
                ex.printStackTrace();
            }

            runOnUiThread(() -> progressDialog.hide());
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
        String meetingLink = meetingLinkEditText.getText().toString();
        if (!meetingLink.startsWith("https://"))
        {
            ShowMessage("Invalid teams meeting link");
            return false;
        }

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