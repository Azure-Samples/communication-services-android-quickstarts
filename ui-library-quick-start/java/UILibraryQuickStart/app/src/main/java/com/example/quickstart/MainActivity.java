package com.example.quickstart;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.common.CommunicationTokenRefreshOptions;
import com.azure.android.communication.toolkit.CallingCompositeBuilder;
import com.azure.android.communication.toolkit.CallingComposite;
import com.azure.android.communication.toolkit.GroupCallParameters;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startCallingCompositeButton = findViewById(R.id.startUILibraryButton);

        startCallingCompositeButton.setOnClickListener(l -> {
            startCallingComposite();
        });
    }

    private void startCallingComposite() {
        CallingComposite callingComposite = new CallingCompositeBuilder().build();

        CommunicationTokenRefreshOptions communicationTokenRefreshOptions =
                new CommunicationTokenRefreshOptions(this::fetchToken, true);
        CommunicationTokenCredential communicationTokenCredential = new CommunicationTokenCredential(communicationTokenRefreshOptions);

        GroupCallParameters parameters = new GroupCallParameters(this,
                communicationTokenCredential,
                "DISPLAY_NAME",
                UUID.fromString("GROUP_CALL_ID"));

        callingComposite.startExperience(parameters);
    }

    private String fetchToken() {
        return "USER_ACCESS_TOKEN";
    }
}