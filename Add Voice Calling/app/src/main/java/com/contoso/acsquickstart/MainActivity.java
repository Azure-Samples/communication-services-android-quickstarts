package com.contoso.acsquickstart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import com.azure.android.communication.calling.TeamsCallAgentOptions;
import com.azure.android.communication.common.CommunicationCloudEnvironment;
import com.azure.android.communication.common.CommunicationIdentifier;
import com.azure.android.communication.common.CommunicationUserIdentifier;
import com.azure.android.communication.calling.Call;
import com.azure.android.communication.calling.TeamsCall;
import com.azure.android.communication.calling.CallAgent;
import com.azure.android.communication.calling.TeamsCallAgent;
import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.HangUpOptions;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.calling.StartCallOptions;
import com.azure.android.communication.calling.StartTeamsCallOptions;
import com.azure.android.communication.common.MicrosoftTeamsUserIdentifier;

public class MainActivity extends AppCompatActivity {
    private static final String[] allPermissions = new String[] { Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE };
    private static final String UserToken = "<User_Access_Token>";
    private static final String TeamsUserToken = "<Teams_User_Access_Token>";

    TextView statusBar;

    private CallAgent agent;
    private TeamsCallAgent teamsAgent;
    private Call call;
    private TeamsCall teamsCall;
    private Button callButton;
    RadioButton acsCall, cteCall;

    private boolean isCte = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        callButton = findViewById(R.id.call_button);

        getAllPermissions();
        setupAgent();

        acsCall = findViewById(R.id.acs_call);
        acsCall.setOnClickListener(this::onCallMethodChanged);
        acsCall.setChecked(true);
        cteCall = findViewById(R.id.cte_call);
        cteCall.setOnClickListener(this::onCallMethodChanged);

        Button hangupButton = findViewById(R.id.hangup_button);
        hangupButton.setOnClickListener(l -> endCall());

        statusBar = findViewById(R.id.status_bar);
        
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    /**
     * Start a call
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
        ArrayList<CommunicationIdentifier> participants = new ArrayList<>();
        participants.add(new CommunicationUserIdentifier(calleeId));
        StartCallOptions options = new StartCallOptions();
        call = agent.startCall(
                getApplicationContext(),
                participants,
                options);
        call.addOnStateChangedListener(p -> setStatus(call.getState().toString()));
    }

    /**
     * Start a teams call
     */
    private void startTeamsCall() {
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
        StartTeamsCallOptions options = new StartTeamsCallOptions();
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
            if(isCte) {
                teamsCall.hangUp(new HangUpOptions()).get();
            }else {
                call.hangUp(new HangUpOptions()).get();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, "Unable to hang up call", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getApplicationContext(), "Failed to create call agent.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAgent(){
        if(isCte){
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
        setupCallButton();
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

    private void setupCallButton(){
        if (isCte){
            callButton.setOnClickListener(l -> startTeamsCall());
        }else {
            callButton.setOnClickListener(l -> startCall());
        }
    }

    /**
     * Shows message in the status bar
     */
    private void setStatus(String status) {
        runOnUiThread(() -> statusBar.setText(status));
    }

    private void onCallMethodChanged(View view){
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()){
            case R.id.acs_call:
                if (checked)
                    isCte = false;
                break;
            case R.id.cte_call:
                if (checked)
                    isCte = true;
                break;
        }
        setupAgent();
    }
}
