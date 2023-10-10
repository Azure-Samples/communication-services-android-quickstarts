package com.contoso.acsquickstart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import com.azure.android.communication.calling.Call;
import com.azure.android.communication.calling.CallAgent;
import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.HangUpOptions;
import com.azure.android.communication.calling.JoinCallOptions;
import com.azure.android.communication.calling.TeamsCall;
import com.azure.android.communication.calling.TeamsCallAgent;
import com.azure.android.communication.calling.TeamsCallAgentOptions;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.calling.TeamsMeetingLinkLocator;

public class MainActivity extends AppCompatActivity {
    private static final String[] allPermissions = new String[] { Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE };
    private static final String UserToken = "<User_Access_Token>";
    private static final String TeamsUserToken = "<Teams_User_Access_Token>";

    TextView callStatusBar;
    TextView recordingStatusBar;

    private CallAgent agent;
    private TeamsCallAgent teamsAgent;
    private Call call;
    private TeamsCall teamsCall;

    RadioButton acsCall;
    RadioButton cteCall;

    private boolean isCTE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        acsCall = findViewById(R.id.acs_call);
        acsCall.setOnClickListener(this::callMethodChanged);
        acsCall.setChecked(true);
        cteCall = findViewById(R.id.cte_call);
        cteCall.setOnClickListener(this::callMethodChanged);

        getAllPermissions();
        setupAgent();

        Button joinMeetingButton = findViewById(R.id.join_meeting_button);
        joinMeetingButton.setOnClickListener(l -> joinTeamsMeeting());

        Button hangupButton = findViewById(R.id.hangup_button);
        hangupButton.setOnClickListener(l -> leaveMeeting());

        callStatusBar = findViewById(R.id.call_status_bar);
        recordingStatusBar = findViewById(R.id.recording_status_bar);
    }

    /**
     * Join Teams meeting
     */
    private void joinTeamsMeeting() {
        if (UserToken.startsWith("<") || TeamsUserToken.startsWith("<")) {
            Toast.makeText(this, "Please enter token in source code", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText calleeIdView = findViewById(R.id.teams_meeting_link);
        String meetingLink = calleeIdView.getText().toString();
        if (meetingLink.isEmpty()) {
            Toast.makeText(this, "Please enter Teams meeting link", Toast.LENGTH_SHORT).show();
            return;
        }

        JoinCallOptions options = new JoinCallOptions();
        TeamsMeetingLinkLocator teamsMeetingLinkLocator = new TeamsMeetingLinkLocator(meetingLink);

        if (isCTE){
            teamsCall = teamsAgent.join(
                    getApplicationContext(),
                    teamsMeetingLinkLocator,
                    options);
            teamsCall.addOnStateChangedListener((p -> setCallStatus(teamsCall.getState().toString())));
        }else {
            call = agent.join(
                    getApplicationContext(),
                    teamsMeetingLinkLocator,
                    options);
            call.addOnStateChangedListener(p -> setCallStatus(call.getState().toString()));
        }
    }

    /**
     * Leave from the meeting
     */
    private void leaveMeeting() {
        try {
            if (isCTE){
                teamsCall.hangUp(new HangUpOptions()).get();
            }else {
                call.hangUp(new HangUpOptions()).get();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, "Unable to leave meeting", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create the call agent
     */
    private void createAgent() {
        try {
            CommunicationTokenCredential credential = new CommunicationTokenCredential(UserToken);
            agent = new CallClient().createCallAgent(getApplicationContext(), credential).get();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Failed to create call agent.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create the teams call agent
     */
    private void createTeamsAgent() {
        try {
            CommunicationTokenCredential credential = new CommunicationTokenCredential(TeamsUserToken);
            TeamsCallAgentOptions options = new TeamsCallAgentOptions();
            teamsAgent = new CallClient().createTeamsCallAgent(getApplicationContext(), credential, options).get();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Failed to create teams call agent.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAgent(){
        if(isCTE){
            if (agent != null) {
                agent.dispose();
                agent = null;
            }
            createTeamsAgent();
        }else{
            if (teamsAgent != null) {
                teamsAgent.dispose();
                teamsAgent = null;
            }
            createAgent();
        }
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
     * Shows call status in status bar
     */
    private void setCallStatus(String status) {
        runOnUiThread(() -> callStatusBar.setText(status));
    }

    /**
     * Shows recording status status bar
     */
    private void setRecordingStatus(boolean status) {
        if (status == true) {
            runOnUiThread(() -> recordingStatusBar.setText("This call is being recorded"));
        }
        else {
            runOnUiThread(() -> recordingStatusBar.setText(""));
        }
    }

    private void callMethodChanged(View view){
        switch (view.getId()){
            case R.id.acs_call:
                if (((RadioButton) view).isChecked()){
                    isCTE = false;
                }
                break;
            case R.id.cte_call:
                if (((RadioButton) view).isChecked()){
                    isCTE = true;
                }
                break;
        }
        setupAgent();
    }
}