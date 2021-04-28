package com.example.videocallingquickstart;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.content.Context;
import com.azure.android.communication.calling.CallState;
import com.azure.android.communication.calling.CallingCommunicationException;
import com.azure.android.communication.calling.CameraFacing;
import com.azure.android.communication.calling.PropertyChangedEvent;
import com.azure.android.communication.calling.VideoDeviceInfo;
import com.azure.android.communication.common.CommunicationUserIdentifier;
import com.azure.android.communication.common.CommunicationIdentifier;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.calling.CallAgent;
import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.StartCallOptions;
import com.azure.android.communication.calling.DeviceManager;
import com.azure.android.communication.calling.VideoOptions;
import com.azure.android.communication.calling.LocalVideoStream;
import com.azure.android.communication.calling.VideoStreamRenderer;
import com.azure.android.communication.calling.VideoStreamRendererView;
import com.azure.android.communication.calling.CreateViewOptions;
import com.azure.android.communication.calling.ScalingMode;
import com.azure.android.communication.calling.IncomingCall;
import com.azure.android.communication.calling.Call;
import com.azure.android.communication.calling.AcceptCallOptions;
import com.azure.android.communication.calling.ParticipantsUpdatedEvent;
import com.azure.android.communication.calling.RemoteParticipant;
import com.azure.android.communication.calling.RemoteVideoStream;
import com.azure.android.communication.calling.RemoteVideoStreamsEvent;
import com.azure.android.communication.calling.RendererListener;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private CallAgent callAgent;
    private LocalVideoStream currentVideoStream;
    private DeviceManager deviceManager;
    private IncomingCall incomingCall;
    private Call call;
    VideoStreamRenderer previewRenderer;
    VideoStreamRendererView preview;
    final Map<Integer, StreamData> streamData = new HashMap<>();
    private boolean renderRemoteVideo = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getAllPermissions();
        createAgent();

        handleIncomingCall();

        Button callButton = findViewById(R.id.call_button);
        callButton.setOnClickListener(l -> startCall());
        Button hangupButton = findViewById(R.id.hang_up);
        hangupButton.setOnClickListener(l -> hangUp());
        Button startVideo = findViewById(R.id.show_preview);
        startVideo.setOnClickListener(l -> turnOnLocalVideo());
        Button stopVideo = findViewById(R.id.hide_preview);
        stopVideo.setOnClickListener(l -> turnOffLocalVideo());
    }

    private void getAllPermissions() {
        String[] requiredPermissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE};
        ArrayList<String> permissionsToAskFor = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToAskFor.add(permission);
            }
        }
        if (!permissionsToAskFor.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToAskFor.toArray(new String[0]), 1);
        }
    }

    private void createAgent() {
        Context context = this.getApplicationContext();
        String userToken = "<USER_ACCESS_TOKEN>";
        try {
            CommunicationTokenCredential credential = new CommunicationTokenCredential(userToken);
            CallClient callClient = new CallClient();
            deviceManager = callClient.getDeviceManager(context).get();
            callAgent = callClient.createCallAgent(getApplicationContext(), credential).get();
        } catch (Exception ex) {
            Toast.makeText(context, "Failed to create call agent.", Toast.LENGTH_SHORT).show();
        }
    }
    private void handleIncomingCall() {
        callAgent.addOnIncomingCallListener((incomingCall) -> {
            this.incomingCall = incomingCall;
            Executors.newCachedThreadPool().submit(this::answerIncomingCall);
        });
    }

    private void startCall() {
        Context context = this.getApplicationContext();
        EditText calleeIdView = findViewById(R.id.callee_id);
        String calleeId = calleeIdView.getText().toString();
        ArrayList<CommunicationIdentifier> participants = new ArrayList<CommunicationIdentifier>();
        StartCallOptions options = new StartCallOptions();
        List<VideoDeviceInfo> cameras = deviceManager.getCameras();
        if(!cameras.isEmpty()) {
            VideoDeviceInfo camera = chooseCamera(cameras);
            currentVideoStream = new LocalVideoStream(camera, context);
            LocalVideoStream[] videoStreams = new LocalVideoStream[1];
            videoStreams[0] = currentVideoStream;
            VideoOptions videoOptions = new VideoOptions(videoStreams);
            options.setVideoOptions(videoOptions);
            showPreview(currentVideoStream);
        }
        participants.add(new CommunicationUserIdentifier(calleeId));

        call = callAgent.startCall(
                context,
                participants,
                options);
        call.addOnRemoteParticipantsUpdatedListener(this::handleRemoteParticipantsUpdate);
        call.addOnStateChangedListener(this::handleCallOnStateChanged);
    }

    private void hangUp() {
        try {
            call.hangUp().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        if (previewRenderer != null) {
            previewRenderer.dispose();
        }
    }

    public void turnOnLocalVideo() {
        List<VideoDeviceInfo> cameras = deviceManager.getCameras();
        if(!cameras.isEmpty()) {
            try {
                VideoDeviceInfo cameraToUse = chooseCamera(cameras);
                currentVideoStream = new LocalVideoStream(cameraToUse, this);
                showPreview(currentVideoStream);
                call.startVideo(this, currentVideoStream).get();
            } catch (CallingCommunicationException acsException) {
                acsException.printStackTrace();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void turnOffLocalVideo() {
        try {
            ((LinearLayout) findViewById(R.id.localvideocontainer)).removeAllViews();
            previewRenderer.dispose();
            previewRenderer = null;
            call.stopVideo(this, currentVideoStream).get();
        } catch (CallingCommunicationException acsException) {
            acsException.printStackTrace();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    VideoDeviceInfo chooseCamera(List<VideoDeviceInfo> cameras) {
        for (VideoDeviceInfo camera : cameras) {
            if (camera.getCameraFacing() == CameraFacing.FRONT) {
                return camera;
            }
        }
        return cameras.get(0);
    }

    private void showPreview(LocalVideoStream stream) {
        // Create renderer
        previewRenderer = new VideoStreamRenderer(stream, this);
        LinearLayout layout = ((LinearLayout)findViewById(R.id.localvideocontainer));
        preview = previewRenderer.createView(new CreateViewOptions(ScalingMode.FIT));
        runOnUiThread(() -> {
            layout.addView(preview);
        });
    }

    private void handleCallOnStateChanged(PropertyChangedEvent args) {
        if (call.getState() == CallState.CONNECTED) {
            handleCallState();
        }
        if (call.getState() == CallState.DISCONNECTED) {
            if (previewRenderer != null) {
                previewRenderer.dispose();
            }
        }
    }

    private void handleCallState() {
        LinearLayout participantVideoContainer = findViewById(R.id.remotevideocontainer);
        handleAddedParticipants(call.getRemoteParticipants(), participantVideoContainer);
    }

    private void answerIncomingCall() {
        Context context = this.getApplicationContext();
        if (incomingCall == null) {
            return;
        }
        AcceptCallOptions acceptCallOptions = new AcceptCallOptions();
        List<VideoDeviceInfo> cameras = deviceManager.getCameras();
        if(!cameras.isEmpty()) {
            VideoDeviceInfo camera = chooseCamera(cameras);
            currentVideoStream = new LocalVideoStream(camera, context);
            LocalVideoStream[] videoStreams = new LocalVideoStream[1];
            videoStreams[0] = currentVideoStream;
            VideoOptions videoOptions = new VideoOptions(videoStreams);
            acceptCallOptions.setVideoOptions(videoOptions);
            showPreview(currentVideoStream);
        }
        try {
            call = incomingCall.accept(context, acceptCallOptions).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        call.addOnRemoteParticipantsUpdatedListener(this::handleRemoteParticipantsUpdate);
        call.addOnStateChangedListener(this::handleCallOnStateChanged);
    }

    public void handleRemoteParticipantsUpdate(ParticipantsUpdatedEvent args) {
        LinearLayout participantVideoContainer = findViewById(R.id.remotevideocontainer);
        handleAddedParticipants(args.getAddedParticipants(), participantVideoContainer);
    }

    private void handleAddedParticipants(List<RemoteParticipant> participants, LinearLayout participantVideoContainer) {
        for (RemoteParticipant remoteParticipant : participants) {
            remoteParticipant.addOnVideoStreamsUpdatedListener(videoStreamsEventArgs -> videoStreamsUpdated(videoStreamsEventArgs));
        }
    }

    private void videoStreamsUpdated(RemoteVideoStreamsEvent videoStreamsEventArgs) {
        for(RemoteVideoStream stream : videoStreamsEventArgs.getAddedRemoteVideoStreams()) {
            StreamData data = new StreamData(stream, null, null);
            streamData.put(stream.getId(), data);
            if (renderRemoteVideo) {
                startRenderingVideo(data);
            }
        }

        for(RemoteVideoStream stream : videoStreamsEventArgs.getRemovedRemoteVideoStreams()) {
            stopRenderingVideo(stream);
        }
    }

    void startRenderingVideo(StreamData data){
        if (data.renderer != null) {
            return;
        }
        LinearLayout layout = ((LinearLayout)findViewById(R.id.remotevideocontainer));
        data.renderer = new VideoStreamRenderer(data.stream, this);
        data.renderer.addRendererListener(new RendererListener() {
            @Override
            public void onFirstFrameRendered() {
                String text = data.renderer.getSize().toString();
                Log.i("MainActivity", "Video rendering at: " + text);
            }

            @Override
            public void onRendererFailedToStart() {
                String text = "Video failed to render";
                Log.i("MainActivity", text);
            }
        });
        data.rendererView = data.renderer.createView(new CreateViewOptions(ScalingMode.FIT));
        runOnUiThread(() -> {
            layout.addView(data.rendererView);
        });
    }

    void stopRenderingVideo(RemoteVideoStream stream) {
        StreamData data = streamData.get(stream.getId());
        if (data == null || data.renderer == null) {
            return;
        }
        runOnUiThread(() -> {
            ((LinearLayout) findViewById(R.id.remotevideocontainer)).removeAllViews();
        });
        data.rendererView = null;
        // Dispose renderer
        data.renderer.dispose();
        data.renderer = null;
    }

    static class StreamData {
        RemoteVideoStream stream;
        VideoStreamRenderer renderer;
        VideoStreamRendererView rendererView;
        StreamData(RemoteVideoStream stream, VideoStreamRenderer renderer, VideoStreamRendererView rendererView) {
            this.stream = stream;
            this.renderer = renderer;
            this.rendererView = rendererView;
        }
    }
}
