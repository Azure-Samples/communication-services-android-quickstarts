package com.example.videocallingquickstart;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.content.Context;
import com.azure.android.communication.calling.CallState;
import com.azure.android.communication.calling.CallingCommunicationException;
import com.azure.android.communication.calling.GroupCallLocator;
import com.azure.android.communication.calling.IncomingAudioOptions;
import com.azure.android.communication.calling.IncomingVideoOptions;
import com.azure.android.communication.calling.JoinCallOptions;
import com.azure.android.communication.calling.OutgoingAudioOptions;
import com.azure.android.communication.calling.OutgoingVideoOptions;
import com.azure.android.communication.calling.ParticipantsUpdatedListener;
import com.azure.android.communication.calling.PropertyChangedEvent;
import com.azure.android.communication.calling.PropertyChangedListener;
import com.azure.android.communication.calling.StartCallOptions;
import com.azure.android.communication.calling.StartTeamsCallOptions;
import com.azure.android.communication.calling.TeamsCallAgentOptions;
import com.azure.android.communication.calling.VideoDeviceInfo;
import com.azure.android.communication.calling.VideoStreamType;
import com.azure.android.communication.common.CommunicationIdentifier;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.calling.CallAgent;
import com.azure.android.communication.calling.TeamsCallAgent;
import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.DeviceManager;
import com.azure.android.communication.calling.VideoOptions;
import com.azure.android.communication.calling.LocalVideoStream;
import com.azure.android.communication.calling.VideoStreamRenderer;
import com.azure.android.communication.calling.VideoStreamRendererView;
import com.azure.android.communication.calling.CreateViewOptions;
import com.azure.android.communication.calling.ScalingMode;
import com.azure.android.communication.calling.IncomingCall;
import com.azure.android.communication.calling.TeamsIncomingCall;
import com.azure.android.communication.calling.Call;
import com.azure.android.communication.calling.TeamsCall;
import com.azure.android.communication.calling.AcceptCallOptions;
import com.azure.android.communication.calling.ParticipantsUpdatedEvent;
import com.azure.android.communication.calling.RemoteParticipant;
import com.azure.android.communication.calling.RemoteVideoStream;
import com.azure.android.communication.calling.RemoteVideoStreamsEvent;
import com.azure.android.communication.calling.RendererListener;
import com.azure.android.communication.common.CommunicationUserIdentifier;
import com.azure.android.communication.common.MicrosoftTeamsUserIdentifier;
import com.azure.android.communication.common.PhoneNumberIdentifier;
import com.azure.android.communication.common.UnknownIdentifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private CallAgent callAgent;
    private TeamsCallAgent teamsCallAgent;
    private VideoDeviceInfo currentCamera;
    private LocalVideoStream currentVideoStream;
    private DeviceManager deviceManager;
    private IncomingCall incomingCall;
    private TeamsIncomingCall teamsIncomingCall;
    private Call call;
    private TeamsCall teamsCall;
    VideoStreamRenderer previewRenderer;
    VideoStreamRendererView preview;
    final Map<Integer, StreamData> streamData = new HashMap<>();
    private boolean renderRemoteVideo = true;
    private ParticipantsUpdatedListener remoteParticipantUpdatedListener;
    private PropertyChangedListener onStateChangedListener;

    final HashSet<String> joinedParticipants = new HashSet<>();

    Button switchSourceButton;
    RadioButton acsCall, cteCall, oneToOneCall, groupCall;
    private boolean isCte = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getAllPermissions();
        createAgent();
        createTeamsAgent();

        handleIncomingCall();
        handleTeamsIncomingCall();

        switchSourceButton = findViewById(R.id.switch_source);
        switchSourceButton.setOnClickListener(l -> switchSource());

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        acsCall = findViewById(R.id.acs_call);
        acsCall.setOnClickListener(this::onCallTypeSelected);
        cteCall = findViewById(R.id.cte_call);
        cteCall.setOnClickListener(this::onCallTypeSelected);
        cteCall.setChecked(true);

        setupButtonListener();
        Button hangupButton = findViewById(R.id.hang_up);
        hangupButton.setOnClickListener(l -> hangUp());
        Button startVideo = findViewById(R.id.show_preview);
        startVideo.setOnClickListener(l -> turnOnLocalVideo());
        Button stopVideo = findViewById(R.id.hide_preview);
        stopVideo.setOnClickListener(l -> turnOffLocalVideo());

        oneToOneCall = findViewById(R.id.one_to_one_call);
        oneToOneCall.setOnClickListener(this::onCallTypeSelected);
        oneToOneCall.setChecked(true);
        groupCall = findViewById(R.id.group_call);
        groupCall.setOnClickListener(this::onCallTypeSelected);

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
        String userToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjVFODQ4MjE0Qzc3MDczQUU1QzJCREU1Q0NENTQ0ODlEREYyQzRDODQiLCJ4NXQiOiJYb1NDRk1kd2M2NWNLOTVjelZSSW5kOHNUSVEiLCJ0eXAiOiJKV1QifQ.eyJza3lwZWlkIjoiYWNzOmVmZDNjMjI5LWIyMTItNDM3YS05NDVkLTkyMzI2ZjEzYTFiZV8wMDAwMDAxYS1jZDllLWQ4ODAtMGQ4Yi0wODQ4MjIwMGJmMjAiLCJzY3AiOjE3OTIsImNzaSI6IjE2OTI5ODQ0ODkiLCJleHAiOjE2OTMwNzA4ODksInJnbiI6ImFtZXIiLCJhY3NTY29wZSI6InZvaXAiLCJyZXNvdXJjZUlkIjoiZWZkM2MyMjktYjIxMi00MzdhLTk0NWQtOTIzMjZmMTNhMWJlIiwicmVzb3VyY2VMb2NhdGlvbiI6InVuaXRlZHN0YXRlcyIsImlhdCI6MTY5Mjk4NDQ4OX0.JfEsYxnMxc9G3cScHEvi_QXQu0aeQk7aSRPSUvSd0QgEfCcQQ8D2XJm0U21AuWpzq50LHEm1FnHU1mU5VbFFy7hWO3UWBz2GYErwTRKTcRUsvwH_SBi46Nf11saYYhfHLYb3S6WT1mnVfJm2-PVWqPwK_SjCbiUypEEWPM4dgohsLiIiVBGoGB-OhcF-B7L51orqgy883vQfevPlQicudr3J8noh7nn5wVGRqkJjyr-jkKPIdjT9iMLssbpfMqpi5DIgYapKYGHSzFCrfMW0PsheAwL4b5CW9ml_fJdr5ooCepLFvwFmYaT7nlF5rWQ7SKXr8MhnFILZwUs3pt7lSA";
        try {
            CommunicationTokenCredential credential = new CommunicationTokenCredential(userToken);
            CallClient callClient = new CallClient();
            deviceManager = callClient.getDeviceManager(context).get();
            callAgent = callClient.createCallAgent(getApplicationContext(), credential).get();
        } catch (Exception ex) {
            Toast.makeText(context, "Failed to create call agent.", Toast.LENGTH_SHORT).show();
        }
    }

    private void createTeamsAgent() {
        Context context = this.getApplicationContext();
        String userToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjVFODQ4MjE0Qzc3MDczQUU1QzJCREU1Q0NENTQ0ODlEREYyQzRDODQiLCJ4NXQiOiJYb1NDRk1kd2M2NWNLOTVjelZSSW5kOHNUSVEiLCJ0eXAiOiJKV1QifQ.eyJza3lwZWlkIjoib3JnaWQ6YjA0YmQ4N2QtYTY4Yi00ODc2LTk2OTYtYjNhMjkwOWYzZjcxIiwic2NwIjoxMDI0LCJjc2kiOiIxNjkyOTg0MjY0IiwiZXhwIjoxNjkyOTg5MTU5LCJyZ24iOiJhbWVyIiwidGlkIjoiYmM2MWY0ZmMtMjZkNy00MTFlLTkxYTktNGMxNDY5MWRhYmRmIiwiYWNzU2NvcGUiOiJ2b2lwLGNoYXQiLCJyZXNvdXJjZUlkIjoiZWZkM2MyMjktYjIxMi00MzdhLTk0NWQtOTIzMjZmMTNhMWJlIiwiaWF0IjoxNjkyOTg0NTY0fQ.R2Pryev1UqS1v0M-sFeRFQbtnfNkyqI_8eEhI0WGZFxco4uT90JDo2jbaQy-7fxS5CrPUEktLP0T8a_dCsHa-tv-P0T0cV13SM5ry8fbaV4zAPW78OROLt2oqYTW-JhA7JHVM2DzFP32IYh8jQYl22OMZO_rsbVkP9rQg4EdyKweKKNtxxeHwNCLJfDWMXmQnE8ZcWjTGSu-9sbjnXijRobjgQxLkqKgPGkcoQh56nca-BvR2Wr_f5GBk-xzSrW2KRThSWEyPcGIXMjRKHjPRDIWUqSfCNNEZvMeNk4HGvK9b6k8mo7eucA7IWvoiaXQqbUz_IGnZ9U4AkZSxxfC5A";
        try {
            CommunicationTokenCredential credential = new CommunicationTokenCredential(userToken);
            CallClient callClient = new CallClient();
            TeamsCallAgentOptions teamsCallAgentOptions = new TeamsCallAgentOptions();
            deviceManager = callClient.getDeviceManager(context).get();
            teamsCallAgent = callClient.createTeamsCallAgent(getApplicationContext(), credential, teamsCallAgentOptions).get();
        } catch (Exception ex) {
            Toast.makeText(context, "Failed to create teams call agent.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleIncomingCall() {
        callAgent.addOnIncomingCallListener((incomingCall) -> {
            this.incomingCall = incomingCall;
            Executors.newCachedThreadPool().submit(this::answerIncomingCall);
        });
    }

    private void handleTeamsIncomingCall() {
        teamsCallAgent.addOnIncomingCallListener((incomingCall) -> {
            this.teamsIncomingCall = incomingCall;
            Executors.newCachedThreadPool().submit(this::answerTeamsIncomingCall);
        });
    }

    private void startCall() {
        Context context = this.getApplicationContext();
        EditText callIdView = findViewById(R.id.call_id);
        String callId = callIdView.getText().toString();
        ArrayList<CommunicationIdentifier> participants = new ArrayList<CommunicationIdentifier>();
        List<VideoDeviceInfo> cameras = deviceManager.getCameras();


        if(oneToOneCall.isChecked()){
            StartCallOptions options = new StartCallOptions();
            IncomingVideoOptions incomingVideoOptions = new IncomingVideoOptions();
            OutgoingVideoOptions outgoingVideoOptions = new OutgoingVideoOptions();
            OutgoingAudioOptions outgoingAudioOptions = new OutgoingAudioOptions();
            if(!cameras.isEmpty()) {
                currentCamera = getNextAvailableCamera(null);
                currentVideoStream = new LocalVideoStream(currentCamera, context);
                LocalVideoStream[] videoStreams = new LocalVideoStream[1];
                videoStreams[0] = currentVideoStream;
                incomingVideoOptions.setStreamType(VideoStreamType.REMOTE_INCOMING);
                outgoingVideoOptions.setOutgoingVideoStreams(Arrays.asList(videoStreams[0]));
                outgoingAudioOptions.setMuted(false);
                showPreview(currentVideoStream);
            }
            participants.add(new CommunicationUserIdentifier(callId));

            options.setIncomingVideoOptions(incomingVideoOptions);
            options.setOutgoingVideoOptions(outgoingVideoOptions);
            options.setOutgoingAudioOptions(outgoingAudioOptions);

            call = callAgent.startCall(
                    context,
                    participants,
                    options);
        }
        else{

            JoinCallOptions options = new JoinCallOptions();
            if(!cameras.isEmpty()) {
                currentCamera = getNextAvailableCamera(null);
                currentVideoStream = new LocalVideoStream(currentCamera, context);
                LocalVideoStream[] videoStreams = new LocalVideoStream[1];
                videoStreams[0] = currentVideoStream;
                VideoOptions videoOptions = new VideoOptions(videoStreams);
                options.setVideoOptions(videoOptions);
                showPreview(currentVideoStream);
            }
            GroupCallLocator groupCallLocator = new GroupCallLocator(UUID.fromString(callId));

            call = callAgent.join(
                    context,
                    groupCallLocator,
                    options);
        }
        remoteParticipantUpdatedListener = this::handleRemoteParticipantsUpdate;
        onStateChangedListener = this::handleCallOnStateChanged;
        call.addOnRemoteParticipantsUpdatedListener(remoteParticipantUpdatedListener);
        call.addOnStateChangedListener(onStateChangedListener);
    }

    private void startTeamsCall() {
        Context context = this.getApplicationContext();
        EditText callIdView = findViewById(R.id.call_id);
        String callId = callIdView.getText().toString();
        MicrosoftTeamsUserIdentifier participant = new MicrosoftTeamsUserIdentifier(callId);
        List<VideoDeviceInfo> cameras = deviceManager.getCameras();

        if(oneToOneCall.isChecked()){
            StartTeamsCallOptions options = new StartTeamsCallOptions();
            IncomingVideoOptions incomingVideoOptions = new IncomingVideoOptions();
            OutgoingVideoOptions outgoingVideoOptions = new OutgoingVideoOptions();
            OutgoingAudioOptions outgoingAudioOptions = new OutgoingAudioOptions();
            if(!cameras.isEmpty()) {
                currentCamera = getNextAvailableCamera(null);
                currentVideoStream = new LocalVideoStream(currentCamera, this);
                LocalVideoStream[] videoStreams = new LocalVideoStream[1];
                videoStreams[0] = currentVideoStream;
                incomingVideoOptions.setStreamType(VideoStreamType.REMOTE_INCOMING);
                outgoingVideoOptions.setOutgoingVideoStreams(Arrays.asList(videoStreams[0]));
                outgoingAudioOptions.setMuted(false);
                showPreview(currentVideoStream);
            }

            options.setIncomingVideoOptions(incomingVideoOptions);
            options.setOutgoingVideoOptions(outgoingVideoOptions);
            options.setOutgoingAudioOptions(outgoingAudioOptions);

            teamsCall = teamsCallAgent.startCall(
                    context,
                    participant,
                    options);
        }
        else{
            Toast.makeText(context, "Teams user cannot join a group call", Toast.LENGTH_SHORT).show();
        }

        remoteParticipantUpdatedListener = this::handleRemoteParticipantsUpdate;
        onStateChangedListener = this::handleTeamsCallOnStateChanged;
        teamsCall.addOnRemoteParticipantsUpdatedListener(remoteParticipantUpdatedListener);
        teamsCall.addOnStateChangedListener(onStateChangedListener);
    }

    private void hangUp() {
        renderRemoteVideo = false;
        try {
            if (isCte){
                for(RemoteParticipant participant : teamsCall.getRemoteParticipants()){
                    for (RemoteVideoStream stream : participant.getVideoStreams()){
                        stopRenderingVideo(stream);
                    }
                }
                teamsCall.hangUp().get();
            }else {
                for(RemoteParticipant participant : call.getRemoteParticipants()){
                    for (RemoteVideoStream stream : participant.getVideoStreams()){
                        stopRenderingVideo(stream);
                    }
                }
                call.hangUp().get();
            }
            switchSourceButton.setVisibility(View.INVISIBLE);
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
                currentVideoStream = new LocalVideoStream(currentCamera, this);
                showPreview(currentVideoStream);
                if (isCte){
                    teamsCall.startVideo(this, currentVideoStream).get();
                }else {
                    call.startVideo(this, currentVideoStream).get();
                }
                switchSourceButton.setVisibility(View.VISIBLE);
            } catch (CallingCommunicationException acsException) {
                acsException.printStackTrace();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void turnOffLocalVideo() {
        try {
            LinearLayout container = findViewById(R.id.localvideocontainer);
            for (int i = 0; i < container.getChildCount(); ++i) {
                Object tag = container.getChildAt(i).getTag();
                if (tag != null && (int)tag == 0) {
                    container.removeViewAt(i);
                }
            }
            switchSourceButton.setVisibility(View.INVISIBLE);
            previewRenderer.dispose();
            previewRenderer = null;
            if(isCte){
                teamsCall.stopVideo(this, currentVideoStream).get();
            }else {
                call.stopVideo(this, currentVideoStream).get();
            }
        } catch (CallingCommunicationException acsException) {
            acsException.printStackTrace();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private VideoDeviceInfo getNextAvailableCamera(VideoDeviceInfo camera) {
        List<VideoDeviceInfo> cameras = deviceManager.getCameras();
        int currentIndex = 0;
        if (camera == null) {
            return cameras.isEmpty() ? null : cameras.get(0);
        }

        for (int i = 0; i < cameras.size(); i++) {
            if (camera.getId().equals(cameras.get(i).getId())) {
                currentIndex = i;
                break;
            }
        }
        int newIndex = (currentIndex + 1) % cameras.size();
        return cameras.get(newIndex);
    }

    private void showPreview(LocalVideoStream stream) {
        // Create renderer
        previewRenderer = new VideoStreamRenderer(stream, this);
        LinearLayout layout = findViewById(R.id.localvideocontainer);
        preview = previewRenderer.createView(new CreateViewOptions(ScalingMode.FIT));
        preview.setTag(0);
        runOnUiThread(() -> {
            layout.addView(preview);
            switchSourceButton.setVisibility(View.VISIBLE);
        });
    }

    public void switchSource() {
        if (currentVideoStream != null) {
            try {
                currentCamera = getNextAvailableCamera(currentCamera);
                currentVideoStream.switchSource(currentCamera).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCallOnStateChanged(PropertyChangedEvent args) {
        if (call.getState() == CallState.CONNECTED) {
            runOnUiThread(() -> Toast.makeText(this, "Call is CONNECTED", Toast.LENGTH_SHORT).show());
            handleCallState();
        }
        if (call.getState() == CallState.DISCONNECTED) {
            runOnUiThread(() -> Toast.makeText(this, "Call is DISCONNECTED", Toast.LENGTH_SHORT).show());
            if (previewRenderer != null) {
                previewRenderer.dispose();
            }
            switchSourceButton.setVisibility(View.INVISIBLE);
        }
    }

    private void handleTeamsCallOnStateChanged(PropertyChangedEvent args) {
        if (teamsCall.getState() == CallState.CONNECTED) {
            runOnUiThread(() -> Toast.makeText(this, "Call is CONNECTED", Toast.LENGTH_SHORT).show());
            handleTeamsCallState();
        }
        if (teamsCall.getState() == CallState.DISCONNECTED) {
            runOnUiThread(() -> Toast.makeText(this, "Call is DISCONNECTED", Toast.LENGTH_SHORT).show());
            if (previewRenderer != null) {
                previewRenderer.dispose();
            }
            switchSourceButton.setVisibility(View.INVISIBLE);
        }
    }

    private void handleCallState() {
        handleAddedParticipants(call.getRemoteParticipants());
    }

    private void handleTeamsCallState() {
        handleAddedParticipants(teamsCall.getRemoteParticipants());
    }

    private void answerIncomingCall() {
        Context context = this.getApplicationContext();
        if (incomingCall == null) {
            return;
        }
        AcceptCallOptions acceptCallOptions = new AcceptCallOptions();
        List<VideoDeviceInfo> cameras = deviceManager.getCameras();
        if(!cameras.isEmpty()) {
            currentCamera = getNextAvailableCamera(null);
            currentVideoStream = new LocalVideoStream(currentCamera, context);
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

        remoteParticipantUpdatedListener = this::handleRemoteParticipantsUpdate;
        onStateChangedListener = this::handleCallOnStateChanged;
        call.addOnRemoteParticipantsUpdatedListener(remoteParticipantUpdatedListener);
        call.addOnStateChangedListener(onStateChangedListener);
    }

    private void answerTeamsIncomingCall() {
        Context context = this.getApplicationContext();
        if (teamsIncomingCall == null) {
            return;
        }
        AcceptCallOptions acceptCallOptions = new AcceptCallOptions();
        List<VideoDeviceInfo> cameras = deviceManager.getCameras();
        if(!cameras.isEmpty()) {
            currentCamera = getNextAvailableCamera(null);
            currentVideoStream = new LocalVideoStream(currentCamera, context);
            LocalVideoStream[] videoStreams = new LocalVideoStream[1];
            videoStreams[0] = currentVideoStream;
            VideoOptions videoOptions = new VideoOptions(videoStreams);
            acceptCallOptions.setVideoOptions(videoOptions);
            showPreview(currentVideoStream);
        }
        try {
            teamsCall = teamsIncomingCall.accept(context, acceptCallOptions).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        remoteParticipantUpdatedListener = this::handleRemoteParticipantsUpdate;
        onStateChangedListener = this::handleTeamsCallOnStateChanged;
        teamsCall.addOnRemoteParticipantsUpdatedListener(remoteParticipantUpdatedListener);
        teamsCall.addOnStateChangedListener(onStateChangedListener);
    }

    public void handleRemoteParticipantsUpdate(ParticipantsUpdatedEvent args) {
        handleAddedParticipants(args.getAddedParticipants());
        handleRemovedParticipants(args.getRemovedParticipants());
    }

    private void handleAddedParticipants(List<RemoteParticipant> participants) {
        for (RemoteParticipant remoteParticipant : participants) {
            if(!joinedParticipants.contains(getId(remoteParticipant))) {
                joinedParticipants.add(getId(remoteParticipant));

                if (renderRemoteVideo) {
                    for (RemoteVideoStream stream : remoteParticipant.getVideoStreams()) {
                        StreamData data = new StreamData(stream, null, null);
                        streamData.put(stream.getId(), data);
                        startRenderingVideo(data);
                    }
                }
                remoteParticipant.addOnVideoStreamsUpdatedListener(videoStreamsEventArgs -> videoStreamsUpdated(videoStreamsEventArgs));
            }
        }
    }

    public String getId(final RemoteParticipant remoteParticipant) {
        final CommunicationIdentifier identifier = remoteParticipant.getIdentifier();
        if (identifier instanceof PhoneNumberIdentifier) {
            return ((PhoneNumberIdentifier) identifier).getPhoneNumber();
        } else if (identifier instanceof MicrosoftTeamsUserIdentifier) {
            return ((MicrosoftTeamsUserIdentifier) identifier).getUserId();
        } else if (identifier instanceof CommunicationUserIdentifier) {
            return ((CommunicationUserIdentifier) identifier).getId();
        } else {
            return ((UnknownIdentifier) identifier).getId();
        }
    }

    private void handleRemovedParticipants(List<RemoteParticipant> removedParticipants) {
        for (RemoteParticipant remoteParticipant : removedParticipants) {
            if(joinedParticipants.contains(getId(remoteParticipant))) {
                joinedParticipants.remove(getId(remoteParticipant));
            }
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
        GridLayout layout = ((GridLayout)findViewById(R.id.remotevideocontainer));
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
        data.rendererView.setTag(data.stream.getId());
        runOnUiThread(() -> {
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(layout.getLayoutParams());
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            params.height = (int)(displayMetrics.heightPixels / 2.5);
            params.width = displayMetrics.widthPixels / 2;
            layout.addView(data.rendererView, params);
        });
    }

    void stopRenderingVideo(RemoteVideoStream stream) {
        StreamData data = streamData.get(stream.getId());
        if (data == null || data.renderer == null) {
            return;
        }
        runOnUiThread(() -> {
            GridLayout layout = findViewById(R.id.remotevideocontainer);
            for(int i = 0; i < layout.getChildCount(); ++ i) {
                View childView =  layout.getChildAt(i);
                if ((int)childView.getTag() == data.stream.getId()) {
                    layout.removeViewAt(i);
                }
            }
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

    public void onCallTypeSelected(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        EditText callIdView = findViewById(R.id.call_id);

        switch(view.getId()) {
            case R.id.acs_call:
                if(checked){
                    isCte = false;
                    setupButtonListener();
                }
                break;
            case R.id.cte_call:
                if(checked){
                    isCte = true;
                    setupButtonListener();
                }
                break;
            case R.id.one_to_one_call:
                if (checked){
                    callIdView.setHint("Callee id");
                }
                break;
            case R.id.group_call:
                if (checked){
                    callIdView.setHint("Group Call GUID");
                }
                break;
        }
    }

    private void setupButtonListener(){
        Button callButton = findViewById(R.id.call_button);
        if(isCte) {
            callButton.setOnClickListener(l -> startTeamsCall());
        }else{
            callButton.setOnClickListener(l -> startCall());
        }
    }
}
