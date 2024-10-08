package com.microsoft.acscallingchatsample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java9.util.function.Consumer;

import com.azure.android.communication.calling.CallAgentOptions;
import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.CallState;
import com.azure.android.communication.calling.HangUpOptions;
import com.azure.android.communication.calling.Call;
import com.azure.android.communication.calling.CallAgent;
import com.azure.android.communication.calling.JoinCallOptions;
import com.azure.android.communication.calling.TeamsMeetingLinkLocator;
import com.azure.android.communication.chat.ChatClient;
import com.azure.android.communication.chat.ChatClientBuilder;
import com.azure.android.communication.chat.ChatThreadClient;
import com.azure.android.communication.chat.ChatThreadClientBuilder;
import com.azure.android.communication.chat.models.ChatEvent;
import com.azure.android.communication.chat.models.ChatEventType;
import com.azure.android.communication.chat.models.ChatMessageReceivedEvent;
import com.azure.android.communication.chat.models.ChatMessageType;
import com.azure.android.communication.chat.models.ChatParticipant;
import com.azure.android.communication.chat.models.ParticipantsAddedEvent;
import com.azure.android.communication.chat.models.ParticipantsRemovedEvent;
import com.azure.android.communication.chat.models.RealTimeNotificationCallback;
import com.azure.android.communication.chat.models.SendChatMessageOptions;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.core.http.policy.UserAgentPolicy;

import com.jakewharton.threetenabp.AndroidThreeTen;

public class MainActivity extends AppCompatActivity {
    private static final String[] allPermissions = new String[] {
                                                        Manifest.permission.RECORD_AUDIO,
                                                        Manifest.permission.CAMERA,
                                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                        Manifest.permission.READ_PHONE_STATE };
    // Scope of the token should have both chat and calling
    private static final String acsUserToken = "<ACS_ACCESS_TOKEN>";
    private String acsResourceEndpoint = "https://<ACS_RESOURCE>.communication.azure.com";
    private String teamsMeetingLink = "<TEAMS_MEETING_LINK>";

    TextView statusBar;

    private CallAgent acsAgent;
    private Call acsCall;
    private Button callButton;
    private Button sendChatMessageButton;
    private String senderDisplayName = "AcsCallingChatSample";

    private ChatClient chatClient;
    private ChatThreadClient chatThreadClient;
    private final String sdkVersion = "1.0.0";
    private static final String APPLICATION_ID = "ACSCallingChatSample";
    private static final String SDK_NAME = "com.azure.android:azure-communication-chat";
    private Boolean chatClientInitialized = false;
    private RealTimeNotificationCallback newChatEventListener;
    private RealTimeNotificationCallback participantAddedListener;
    private RealTimeNotificationCallback participantRemovedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        callButton = findViewById(R.id.call_button);

        callButton.setOnClickListener(l -> joinTeamsMeeting());

        sendChatMessageButton = findViewById(R.id.send_button);
        sendChatMessageButton.setOnClickListener(l -> sendChatMessage());
        
        Button hangupButton = findViewById(R.id.hangup_button);
        hangupButton.setOnClickListener(l -> endCall());

        statusBar = findViewById(R.id.status_bar);

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        EditText calleeIdView = findViewById(R.id.teams_meeting_link);
        calleeIdView.setText(teamsMeetingLink);
        initialize();
    }

    /**
     * Send the chat message to the Teams meeting that was joined.
     */
    private void initialize() {
        if (acsAgent != null) {
            return;
        }
        AndroidThreeTen.init(this);
        getAllPermissions();
        createAgent();
    }

    /**
     * Send the chat message to the Teams meeting that was joined.
     */
    private void sendChatMessage() {
        if (!chatClientInitialized) {
            Toast.makeText(this, "Chat is not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText sendMessageTextBox = findViewById(R.id.message_input_edit_text);
        String messageText = sendMessageTextBox.getText().toString();
        SendChatMessageOptions chatMessageOptions = new SendChatMessageOptions()
                                                        .setType(ChatMessageType.TEXT)
                                                        .setContent(messageText)
                                                        .setSenderDisplayName(senderDisplayName);
        try {
            chatThreadClient.sendMessage(chatMessageOptions);
            sendMessageTextBox.setText("");
            Toast.makeText(this, "Message Sent !!", Toast.LENGTH_SHORT).show();
        } catch(Exception ex) {
            Toast.makeText(this, "Failed to send chat message !!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Join the teams meeting
     */
    private void joinTeamsMeeting() {
        if (acsAgent == null) {
            Toast.makeText(this, "CallAgent is null", Toast.LENGTH_SHORT).show();
            return;
        }

        if (acsUserToken.startsWith("<")) {
            Toast.makeText(this, "Please enter token in source code", Toast.LENGTH_SHORT).show();
            return;
        }

        String calleeId = getTeamsMeetingLink();

        if (calleeId.isEmpty() || !calleeId.startsWith("https")) {
            Toast.makeText(this, "Please enter a valid teams meeting link", Toast.LENGTH_SHORT).show();
            return;
        }

        TeamsMeetingLinkLocator meetingLocator = new TeamsMeetingLinkLocator(calleeId);
        JoinCallOptions joinCallOptions = new JoinCallOptions();
        acsCall = acsAgent.join(getApplicationContext(), meetingLocator, joinCallOptions);
        acsCall.addOnStateChangedListener(p ->
                    {
                        CallState callState = acsCall.getState();
                        if (callState == CallState.CONNECTED) {
                            runOnUiThread(() -> {
                                createChatClient();
                            });
                        }
                        setStatus(acsCall.getState().toString());
                    });
    }

    /**
     * Ends the call previously started and remove the chat event listeners
     */
    private void endCall() {
        try {
            acsCall.hangUp(new HangUpOptions()).get();
            LinearLayout chatMessageWindow = findViewById(R.id.chat_messages_linear_layout);
            chatMessageWindow.removeAllViews();
            chatClient.stopRealtimeNotifications();
            chatClient.removeEventHandler(ChatEventType.CHAT_MESSAGE_RECEIVED, newChatEventListener);
            chatClient.removeEventHandler(ChatEventType.PARTICIPANTS_ADDED, participantAddedListener);
            chatClient.removeEventHandler(ChatEventType.PARTICIPANTS_REMOVED, participantRemovedListener);
            chatClientInitialized = false;
            acsCall = null;
            chatClient = null;
            chatThreadClient = null;
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, "Unable to hang up call", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create the ACS call agent
     */
    private void createAgent() {
        try {
            CommunicationTokenCredential credential = new CommunicationTokenCredential(acsUserToken);
            CallAgentOptions callAgentOptions = new CallAgentOptions();
            callAgentOptions.setDisplayName(senderDisplayName);
            acsAgent = new CallClient().createCallAgent(getApplicationContext(), credential, callAgentOptions).get();
            Toast.makeText(getApplicationContext(), "CallAgent created.", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Failed to create call agent.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create Chat Client and attach the appropriate handlers.
     */
    private void createChatClient() {
        if (acsCall.getState() != CallState.CONNECTED) {
            Toast.makeText(getApplicationContext(), "Call state needs to be connected state.", Toast.LENGTH_SHORT).show();
            return;
        }
        String teamsMeetingLink = getTeamsMeetingLink();

        try {
            chatClientInitialized = false;
            chatClient = new ChatClientBuilder()
                                .endpoint(acsResourceEndpoint)
                                .credential(new CommunicationTokenCredential(acsUserToken))
                                .addPolicy(new UserAgentPolicy(APPLICATION_ID, SDK_NAME, sdkVersion))
                                .buildClient();
            Consumer<Throwable> errorHandler = throwable -> {
                // Show a user-friendly message
                Toast.makeText(getApplicationContext(), "Failed to start Real time notification. Please try again.", Toast.LENGTH_SHORT).show();
            };
            chatClient.startRealtimeNotifications(getApplicationContext(), errorHandler);

            newChatEventListener = chatEvent -> {
                ChatMessageReceivedEvent chatMessageReceivedEvent = (ChatMessageReceivedEvent) chatEvent;
                runOnUiThread(() -> {
                    LinearLayout chatMessageWindow = findViewById(R.id.chat_messages_linear_layout);

                    TextView textViewLines = new TextView(getApplicationContext());
                    textViewLines.setText("--------");
                    chatMessageWindow.addView(textViewLines);

                    TextView textView = new TextView(getApplicationContext());
                    textView.setText(chatMessageReceivedEvent.getSender().getRawId() + ": " + chatMessageReceivedEvent.getContent());
                    chatMessageWindow.addView(textView);

                    ScrollView scrollView = findViewById(R.id.chat_messages_scroll_view);
                    scrollView.fullScroll(View.FOCUS_DOWN);
                });
            };

            participantAddedListener = chatEvent -> {
                ParticipantsAddedEvent participantsAddedEvent = (ParticipantsAddedEvent) chatEvent;
                List<ChatParticipant> participantsAdded = participantsAddedEvent.getParticipantsAdded();
                participantsAdded.forEach(participant ->{
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Participant added: " + participant.getDisplayName(), Toast.LENGTH_LONG).show();
                    });
                });
            };

            participantRemovedListener = chatEvent -> {
                ParticipantsRemovedEvent participantsRemovedEvent = (ParticipantsRemovedEvent) chatEvent;
                List<ChatParticipant> participantsRemoved = participantsRemovedEvent.getParticipantsRemoved();
                participantsRemoved.forEach(participant ->{
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Participant removed: " + participant.getDisplayName(), Toast.LENGTH_LONG).show();
                    });
                });
            };

            chatClient.addEventHandler(ChatEventType.CHAT_MESSAGE_RECEIVED, newChatEventListener);
            chatClient.addEventHandler(ChatEventType.PARTICIPANTS_ADDED, participantAddedListener);
            chatClient.addEventHandler(ChatEventType.PARTICIPANTS_REMOVED, participantRemovedListener);

            String threadId = extractThreadIdFromMeetingLink(teamsMeetingLink);
            chatThreadClient = new ChatThreadClientBuilder()
                                .endpoint(acsResourceEndpoint)
                                .credential(new CommunicationTokenCredential(acsUserToken))
                                .addPolicy(new UserAgentPolicy(APPLICATION_ID, SDK_NAME, sdkVersion))
                                .chatThreadId(threadId)
                                .buildClient();

            chatClientInitialized = true;
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "ChatClient created.", Toast.LENGTH_SHORT).show());
        } catch (Exception ex) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to start Chat client.", Toast.LENGTH_SHORT).show());
            chatClientInitialized = false;
        }
    }

    /**
     * Extract threadId from the meeting link, otherwise sending chat message fails.
     */
    private String extractThreadIdFromMeetingLink(String meetingLink) {
        Uri uri = Uri.parse(meetingLink);
        String path = uri.getPath();
        return path.split("/")[3];
    }

    /**
     * Request each required permission if the app doesn't already have it.
     */
    private void getAllPermissions() {
        ArrayList<String> permissionsToAskFor = new ArrayList<>();
        for (String permission : allPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToAskFor.add(permission);
            }
        }
        if (!permissionsToAskFor.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToAskFor.toArray(new String[0]), 1);
        }
    }

    /**
     * Ensure all permissions were granted, otherwise inform the user permissions are missing.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        boolean allPermissionsGranted = true;
        for (int result : grantResults) {
            allPermissionsGranted &= (result == PackageManager.PERMISSION_GRANTED);
        }
        if (!allPermissionsGranted) {
            Toast.makeText(this, "All permissions are needed to make the call.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Shows message in the status bar
     */
    private void setStatus(String status) {
        runOnUiThread(() -> statusBar.setText(status));
    }

    /**
     * Get the the teams meeting link entered in the textbox.
     */
    private String getTeamsMeetingLink() {
        EditText calleeIdView = findViewById(R.id.teams_meeting_link);
        return calleeIdView.getText().toString();
    }
}
