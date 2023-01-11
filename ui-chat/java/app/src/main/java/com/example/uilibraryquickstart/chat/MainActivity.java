package com.example.uilibraryquickstart.chat;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.common.CommunicationTokenRefreshOptions;
import com.azure.android.communication.common.CommunicationUserIdentifier;
import com.azure.android.communication.ui.chat.ChatAdapter;
import com.azure.android.communication.ui.chat.ChatAdapterBuilder;
import com.azure.android.communication.ui.chat.presentation.ChatThreadView;

public class MainActivity extends AppCompatActivity {

    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(l -> {
            CommunicationTokenRefreshOptions communicationTokenRefreshOptions =
                    new CommunicationTokenRefreshOptions(this::accessToken, true);
            CommunicationTokenCredential communicationTokenCredential =
                    new CommunicationTokenCredential(communicationTokenRefreshOptions);
            chatAdapter = new ChatAdapterBuilder()
                    .endpoint(endpoint())
                    .threadId(threadId())
                    .credential(communicationTokenCredential)
                    .identity(new CommunicationUserIdentifier(acsIdentity()))
                    .displayName(displayName())
                    .build();

            try {
                chatAdapter.connect(MainActivity.this).get();
                View chatView = new ChatThreadView(MainActivity.this, chatAdapter);
                addContentView(chatView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
            }
            catch (Exception e){
                String messageCause = "Unknown error";
                if (e.getCause() != null && e.getCause().getMessage() != null){
                    messageCause = e.getCause().getMessage();
                }
                showAlert(messageCause);
            }
        });
    }

    /**
     *
     * @return String endpoint URL from ACS Admin UI, "https://example.domain.com/"
     */
    private String endpoint(){
        return "https://example.domain.com/";
    }

    /**
     *
     * @return String identity of the user joining the chat
     * Looks like "8:acs:a6aada1f-0b1e-47ac-866a-91aae00a1c01_00000015-45ee-bad7-0ea8-923e0d008a89"
     */
    private String acsIdentity(){
        return "";
    }

    /**
     *
     * @return String display name of the user joining the chat
     */
    private String displayName(){
        return "";
    }

    /**
     *
     * @return String secure ACS access token for the current user
     */
    private String accessToken(){
        return "";
    }

    /**
     *
     * @return String id of ACS chat thread to join
     * Looks like "19:AVNnEll25N4KoNtKolnUAhAMu8ntI_Ra03saj0Za0r01@thread.v2"
     */
    private String threadId(){
        return "";
    }

    void showAlert(String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setTitle("Alert")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .show();
            }
        });
    }

}