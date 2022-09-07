package com.microsoft.acs.calling.rawmediaaccess;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.azure.android.communication.calling.Call;
import com.azure.android.communication.calling.CallAgent;
import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.CallingCommunicationException;
import com.azure.android.communication.calling.HangUpOptions;
import com.azure.android.communication.calling.JoinCallOptions;
import com.azure.android.communication.calling.JoinMeetingLocator;
import com.azure.android.communication.calling.OutgoingVideoStream;
import com.azure.android.communication.calling.OutgoingVideoStreamKind;
import com.azure.android.communication.calling.PixelFormat;
import com.azure.android.communication.calling.RawOutgoingVideoStreamOptions;
import com.azure.android.communication.calling.ScreenShareRawOutgoingVideoStream;
import com.azure.android.communication.calling.TeamsMeetingLinkLocator;
import com.azure.android.communication.calling.VideoFormat;
import com.azure.android.communication.calling.VideoFrameKind;
import com.azure.android.communication.calling.VideoOptions;
import com.azure.android.communication.calling.VirtualRawOutgoingVideoStream;
import com.azure.android.communication.common.CommunicationTokenCredential;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText tokenEditText;
    private EditText meetingUrlEditText;
    private CallClient client;
    private CallAgent callAgent;
    private Call call;
    private OutgoingVideoStream outgoingVideoStream;
    private FrameGenerator frameGenerator;
    private RawOutgoingVideoStreamOptions options;
    private int width = 0;
    private int height = 0;
    private boolean callInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GetAllPermissions();
        tokenEditText = findViewById(R.id.TokenEditText);
        meetingUrlEditText = findViewById(R.id.MeetingUrlEditText);

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    private void GetAllPermissions() {

        String[] requiredPermissions = new String[]
                {
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE
                };

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

    private void CreateAgent() {

        String token = tokenEditText.getText().toString();
        try {

            CommunicationTokenCredential credential = new CommunicationTokenCredential(token);
            client = new CallClient();
            callAgent = client.createCallAgent(getApplicationContext(), credential).get();
        } catch (Exception ex) {

            Toast.makeText(getApplicationContext(),
                    "Failed to create call agent",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void StartCallWithVirtualVideo(View view) {

        StartCall(OutgoingVideoStreamKind.VIRTUAL);
    }

    public void StartCallWithScreenShareVideo(View view) {

        StartCall(OutgoingVideoStreamKind.SCREEN_SHARE);
    }

    private void StartCall(OutgoingVideoStreamKind outgoingVideoStreamKind) {

        if (callInProgress) {

            return;
        }

        String meetingLink = meetingUrlEditText.getText().toString();

        CreateAgent();
        VideoOptions videoOptions = CreateVideoOptions(outgoingVideoStreamKind);
        JoinCallOptions joinCallOptions = new JoinCallOptions();
        joinCallOptions.setVideoOptions(videoOptions);
        JoinMeetingLocator locator = new TeamsMeetingLinkLocator(meetingLink);

        try {

            call = callAgent.join(getApplicationContext(), locator, joinCallOptions);
            callInProgress = true;
        } catch (CallingCommunicationException ex) {

            runOnUiThread(() ->
                    Toast.makeText(getApplicationContext(),
                            String.format("StartCall: %s", ex.getMessage()),
                            Toast.LENGTH_LONG)
                            .show());
        }

        Toast.makeText(getApplicationContext(),
                "Started",
                Toast.LENGTH_LONG)
                .show();
    }

    private VideoOptions CreateVideoOptions(OutgoingVideoStreamKind outgoingVideoStreamKind) {

        frameGenerator = new FrameGenerator();
        options = CreateRawOutgoingVideoStreamOptions(frameGenerator);

        if (outgoingVideoStreamKind == OutgoingVideoStreamKind.VIRTUAL) {

            outgoingVideoStream = new VirtualRawOutgoingVideoStream(options);
        } else {

            outgoingVideoStream = new ScreenShareRawOutgoingVideoStream(options);
        }

        return new VideoOptions(new OutgoingVideoStream[] { outgoingVideoStream });
    }

    private RawOutgoingVideoStreamOptions CreateRawOutgoingVideoStreamOptions(FrameGenerator frameGenerator) {

        width = 1280;
        height = 720;

        VideoFormat videoFormat = new VideoFormat();
        videoFormat.setWidth(width);
        videoFormat.setHeight(height);
        videoFormat.setPixelFormat(PixelFormat.RGBA);
        videoFormat.setVideoFrameKind(VideoFrameKind.VIDEO_SOFTWARE);
        videoFormat.setFramesPerSecond(30);
        videoFormat.setStride1(width * 4);

        RawOutgoingVideoStreamOptions options = new RawOutgoingVideoStreamOptions();
        options.setVideoFormats(Arrays.asList(videoFormat));
        options.addOnVideoFrameSenderChangedListener(frameGenerator);

        return options;
    }

    public void EndCall(View view) {

        if (!callInProgress) {

            return;
        }

        Executors.newCachedThreadPool().submit(() -> {
            try {

                if (call != null) {

                    frameGenerator.StopFrameIterator();
                    call.stopVideo(this, outgoingVideoStream);
                    call.hangUp(new HangUpOptions()).get();
                }

                callInProgress = false;
            } catch (ExecutionException | InterruptedException ex) {

                Toast.makeText(getApplicationContext(),
                        "EndCall: ",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });

        Toast.makeText(getApplicationContext(),
                "Stopped",
                Toast.LENGTH_LONG)
                .show();
    }
}