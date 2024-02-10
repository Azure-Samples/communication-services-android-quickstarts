package com.contoso.acsquickstart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.HangUpOptions;
//import com.azure.android.communication.calling.StartTeamsCallOptions;
import com.azure.android.communication.calling.StartTeamsCallOptions;
import com.azure.android.communication.calling.TeamsCall;
import com.azure.android.communication.calling.TeamsCallAgent;
//import com.azure.android.communication.calling.TeamsCallAgentOptions;
import com.azure.android.communication.common.CommunicationCloudEnvironment;
import com.azure.android.communication.common.CommunicationIdentifier;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.common.MicrosoftTeamsUserIdentifier;

public class MainCTEActivity extends AppCompatActivity {
    private static final String[] allPermissions = new String[] { Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE };
    private static final String UserToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjYwNUVCMzFEMzBBMjBEQkRBNTMxODU2MkM4QTM2RDFCMzIyMkE2MTkiLCJ4NXQiOiJZRjZ6SFRDaURiMmxNWVZpeUtOdEd6SWlwaGsiLCJ0eXAiOiJKV1QifQ.eyJza3lwZWlkIjoib3JnaWQ6MGQyZGEwYzUtZTc2My00YTIyLWJiNDYtYWY4MjExMWNjN2FlIiwic2NwIjoxMDI0LCJjc2kiOiIxNzA2NTUxNTgyIiwiZXhwIjoxNzA2NTU3MDE0LCJyZ24iOiJhbWVyIiwidGlkIjoiYmM2MWY0ZmMtMjZkNy00MTFlLTkxYTktNGMxNDY5MWRhYmRmIiwiYWNzU2NvcGUiOiJ2b2lwLGNoYXQiLCJyZXNvdXJjZUlkIjoiZWZkM2MyMjktYjIxMi00MzdhLTk0NWQtOTIzMjZmMTNhMWJlIiwiYWFkX2lhdCI6IjE3MDY1NTE1ODIiLCJhYWRfdXRpIjoidGQtZUg1ejZRMENOcHZxbjhwcC1BQSIsImFhZF9hcHBpZCI6IjFmZDUxMThlLTI1NzYtNDI2My04MTMwLTk1MDMwNjRjODM3YSIsImlhdCI6MTcwNjU1MTg4Mn0.jijqje8NmdQy2XK2k32aSFRy7x4hXlom7aTBJaS-NmSIMinkysA6AsqpY7IEl9hZGasrJ8UqQtycnnk_1KnsWgdDC-d_6kEb5NGMSalXsEqzeONJvETk7EBB4RRMRMhFqR9YqGFgPmIfkHPHdLPC0eTZvOWnESRYYv1H6ziEJkbLJmI5Vl_46XPL881wfmTkoj7HUa6VMAgaWacVJiPNTMerVR6w6YU3bRSpIHTAfjRLp7h56xux1GCtBbfPlME6RmS42WPO_gU8FLupjpTvZEaPsCfx_il_t0nHo9ychjrtwyVpMICLiLMAY-nbRuwFHTPs-Ik4BdLTAINkCvBakQ";

    TextView statusBar;

    private TeamsCallAgent teamsAgent;
    private TeamsCall teamsCall;
    private Button callButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        callButton = findViewById(R.id.call_button);

        getAllPermissions();
        createAgent();
        callButton.setOnClickListener(l -> startCall());

        Button hangupButton = findViewById(R.id.hangup_button);
        hangupButton.setOnClickListener(l -> endCall());

        statusBar = findViewById(R.id.status_bar);

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    /**
     * Start a teams call
     */
    private void startCall() {
        if (UserToken.startsWith("<")) {
            Toast.makeText(this, "Please enter token in source code", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText calleeIdView = findViewById(R.id.callee_id);
        String calleeId = calleeIdView.getText().toString();
        if (calleeId.isEmpty()) {
            Toast.makeText(this, "Please enter callee", Toast.LENGTH_SHORT).show();
            return;
        }

        StartTeamsCallOptions options = new StartTeamsCallOptions();
        ArrayList<CommunicationIdentifier> participants = new ArrayList<>();
        CommunicationIdentifier participant;
        if (calleeId.startsWith("8:orgid:")){
            participant = new MicrosoftTeamsUserIdentifier(calleeId.substring("8:orgid:".length())).setCloudEnvironment(CommunicationCloudEnvironment.PUBLIC);
        } else if (calleeId.startsWith("8:dod:")) {
            participant = new MicrosoftTeamsUserIdentifier(calleeId.substring("8:dod:".length())).setCloudEnvironment(CommunicationCloudEnvironment.DOD);
        } else if (calleeId.startsWith("8:gcch:")) {
            participant = new MicrosoftTeamsUserIdentifier(calleeId.substring("8:gcch:".length())).setCloudEnvironment(CommunicationCloudEnvironment.GCCH);
        } else {
            participant = new MicrosoftTeamsUserIdentifier(calleeId).setCloudEnvironment(CommunicationCloudEnvironment.PUBLIC);
        }

        teamsCall = teamsAgent.startCall(
                getApplicationContext(),
                participant,
                options);
        teamsCall.addOnStateChangedListener(p -> setStatus(teamsCall.getState().toString()));
    }

    /**
     * Ends the call previously started
     */
    private void endCall() {
        try {
            teamsCall.hangUp(new HangUpOptions()).get();
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, "Unable to hang up call", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create the teams call agent
     */
    private void createAgent() {
        try {
            CommunicationTokenCredential credential = new CommunicationTokenCredential(UserToken);

            teamsAgent = new CallClient().createTeamsCallAgent(getApplicationContext(), credential).get();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Failed to create call agent.", Toast.LENGTH_SHORT).show();
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
     * Shows message in the status bar
     */
    private void setStatus(String status) {
        runOnUiThread(() -> statusBar.setText(status));
    }
}
