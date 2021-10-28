package com.microsoft.acs.calling.rawmediaaccess;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.azure.android.communication.calling.CallAgent;
import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.DeviceManager;
import com.azure.android.communication.calling.FrameConfirmation;
import com.azure.android.communication.calling.GroupCallLocator;
import com.azure.android.communication.calling.JoinCallOptions;
import com.azure.android.communication.calling.LocalVideoStream;
import com.azure.android.communication.calling.MediaFrameKind;
import com.azure.android.communication.calling.MediaFrameSender;
import com.azure.android.communication.calling.OutboundVirtualVideoDevice;
import com.azure.android.communication.calling.OutboundVirtualVideoDeviceOptions;
import com.azure.android.communication.calling.PixelFormat;
import com.azure.android.communication.calling.SoftwareBasedVideoFrame;
import com.azure.android.communication.calling.VideoDeviceInfo;
import com.azure.android.communication.calling.VideoFormat;
import com.azure.android.communication.calling.VideoOptions;
import com.azure.android.communication.calling.VirtualDeviceIdentification;
import com.azure.android.communication.calling.VirtualDeviceRunningState;
import com.azure.android.communication.common.CommunicationTokenCredential;

import org.jetbrains.annotations.Contract;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private CallClient m_client;
    private CallAgent m_callAgent;
    private DeviceManager m_deviceManager;
    private LocalVideoStream m_virtualVideoStream;
    private Thread m_frameGeneratorThread;
    private OutboundVirtualVideoDevice m_outboundVirtualVideoDevice;
    private OutboundVirtualVideoDeviceOptions m_options;
    private MediaFrameSender m_mediaFrameSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getAllPermissions();

        // Bind call button to call `startCall`
        Button callButton = findViewById(R.id.button);

        callButton.setOnClickListener(l -> {
            try {
                createAgent();
                startCall();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    /**
     * Request each required permission if the app doesn't already have it.
     */
    private void getAllPermissions() {
        // See section on requesting permissions
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

    /**
     * Create the call agent for placing calls
     */
    private void createAgent() {
        // See section on creating the call agent
        EditText txtToken = findViewById(R.id.txtToken);
        String userToken = txtToken.getText().toString();

        try {
            CommunicationTokenCredential credential = new CommunicationTokenCredential(userToken);
            m_client = new CallClient();
            m_callAgent = m_client.createCallAgent(getApplicationContext(), credential).get();

            m_deviceManager = new CallClient().getDeviceManager(getApplicationContext()).get();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Failed to create call agent.", Toast.LENGTH_SHORT).show();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createOutboundVirtualVideoDevice() throws Exception {
        VirtualDeviceIdentification deviceId = new VirtualDeviceIdentification();
        deviceId.setId("QuickStartVirtualVideoDevice");
        deviceId.setName("My First Virtual Video Device");

        ArrayList<VideoFormat> videoFormats = new ArrayList<VideoFormat>();

        VideoFormat format = new VideoFormat();
        format.setWidth(1280);
        format.setHeight(720);
        format.setPixelFormat(PixelFormat.RGBA);
        format.setMediaFrameKind(MediaFrameKind.VIDEO_SOFTWARE);
        format.setFramesPerSecond(30);
        format.setStride1(1280 * 4);
        videoFormats.add(format);

        m_options = new OutboundVirtualVideoDeviceOptions();
        m_options.setDeviceIdentification(deviceId);
        m_options.setVideoFormats(videoFormats);

        m_options.addOnFlowChangedListener(virtualDeviceFlowControlArgs -> {
            if (virtualDeviceFlowControlArgs.getMediaFrameSender().getRunningState() == VirtualDeviceRunningState.STARTED) {
                m_mediaFrameSender = virtualDeviceFlowControlArgs.getMediaFrameSender();
            } else {
                m_mediaFrameSender = null;
            }
        });

        m_outboundVirtualVideoDevice = m_deviceManager.createOutboundVirtualVideoDevice(m_options).get();
    }

    private void ensureLocalVideoStreamWithVirtualCamera() {
        for (VideoDeviceInfo videoDeviceInfo : m_deviceManager.getCameras()) {
            String deviceId = videoDeviceInfo.getId();
            if (deviceId.equalsIgnoreCase("QuickStartVirtualVideoDevice")) {
                m_virtualVideoStream = new LocalVideoStream(videoDeviceInfo, getApplicationContext());
            }
        }
    }

    /**
     * Place a call to the callee id provided in `callee_id` text input.
     */
    private void startCall() throws Exception {
        // See section on starting the call
        EditText txtCallee = findViewById(R.id.txtCallee);
        String calleeId = txtCallee.getText().toString();

        createOutboundVirtualVideoDevice();

        m_frameGeneratorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    java.nio.ByteBuffer plane1 = null;
                    Random rand = new Random();

                    while (m_outboundVirtualVideoDevice != null) {
                        while (m_mediaFrameSender != null) {
                            if (m_mediaFrameSender.getMediaFrameKind() == MediaFrameKind.VIDEO_SOFTWARE) {
                                SoftwareBasedVideoFrame sender = (SoftwareBasedVideoFrame) m_mediaFrameSender;
                                VideoFormat videoFormat = sender.getVideoFormat();

                                // Gets the timestamp for when the video frame has been created.
                                // This allows better synchronization with audio.
                                int timeStamp = sender.getTimestamp();

                                // Adjusts frame dimensions to the video format that network conditions can manage.
                                if (plane1 == null || videoFormat.getStride1() * videoFormat.getHeight() != plane1.capacity()) {
                                    plane1 = ByteBuffer.allocateDirect(videoFormat.getStride1() * videoFormat.getHeight());
                                    plane1.order(ByteOrder.nativeOrder());
                                }

                                // Generates random gray scaled bands as video frame.
                                int bandsCount = rand.nextInt(15) + 1;
                                int bandBegin = 0;
                                int bandThickness = videoFormat.getHeight() * videoFormat.getStride1() / bandsCount;

                                for (int i = 0; i < bandsCount; ++i) {
                                    byte greyValue = (byte) rand.nextInt(254);
                                    java.util.Arrays.fill(plane1.array(), bandBegin, bandBegin + bandThickness, greyValue);
                                    bandBegin += bandThickness;
                                }

                                // Sends video frame to the other participants in the call.
                                FrameConfirmation fr = sender.sendFrame(plane1, timeStamp).get();

                                // Waits before generating the next video frame.
                                // Video format defines how many frames per second app must generate.
                                Thread.sleep((long) (1000.0f / videoFormat.getFramesPerSecond()));
                            }
                        }

                        // Virtual camera hasn't been created yet.
                        // Let's wait a little bit before checking again.
                        // This is for demo only purpose.
                        // Please use a better synchronization mechanism.
                        Thread.sleep(32);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        m_frameGeneratorThread.start();

        JoinCallOptions joinCallOptions = new JoinCallOptions();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ensureLocalVideoStreamWithVirtualCamera();

        joinCallOptions.setVideoOptions(new VideoOptions(new LocalVideoStream[]{
                m_virtualVideoStream
        }));

        GroupCallLocator locator = new GroupCallLocator(UUID.fromString(calleeId));
        m_callAgent.join(getApplicationContext(), locator, joinCallOptions);
    }
}