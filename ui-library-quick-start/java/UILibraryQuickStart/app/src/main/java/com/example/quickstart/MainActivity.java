package com.example.quickstart;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.common.CommunicationTokenRefreshOptions;
import com.azure.android.communication.ui.calling.CallComposite;
import com.azure.android.communication.ui.calling.CallCompositeBuilder;
import com.azure.android.communication.ui.calling.models.CallCompositeGroupCallLocator;
import com.azure.android.communication.ui.calling.models.CallCompositeJoinLocator;
import com.azure.android.communication.ui.calling.models.CallCompositeLocalOptions;
import com.azure.android.communication.ui.calling.models.CallCompositeParticipantViewData;
import com.azure.android.communication.ui.calling.models.CallCompositeRemoteOptions;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(l -> {
            startCallComposite();
        });
    }

    private void startCallComposite() {
        CommunicationTokenRefreshOptions communicationTokenRefreshOptions =
                new CommunicationTokenRefreshOptions(this::fetchToken, true);
        CommunicationTokenCredential communicationTokenCredential =
                new CommunicationTokenCredential(communicationTokenRefreshOptions);

        final CallCompositeJoinLocator locator = new CallCompositeGroupCallLocator(UUID.fromString("GROUP_CALL_ID"));
        final CallCompositeRemoteOptions remoteOptions =
                new CallCompositeRemoteOptions(locator, communicationTokenCredential, "DISPLAY_NAME");

        /**
        // Optional parameter - localOptions
        //    - to customize participant view data such as avatar image, scale type and display name
        //    - and to customize navigation bar's title and subtitle
        final CallCompositeParticipantViewData participantViewData = new CallCompositeParticipantViewData()
                .setAvatarBitmap((Bitmap) avatarBitmap)
                .setScaleType((ImageView.ScaleType) scaleType)
                .setDisplayName((String) displayName);

        final CallCompositeNavigationBarViewData navigationBarViewData = new CallCompositeNavigationBarViewData()
                .setTitle((String) title)
                .setSubtitle((String) subTitle);

        final CallCompositeLocalOptions localOptions = new CallCompositeLocalOptions()
                .setParticipantViewData(participantViewData)
                .setNavigationBarViewData(navigationBarViewData);

        callComposite.launch(callLauncherActivity, remoteOptions, localOptions);
         */

        CallComposite callComposite = new CallCompositeBuilder().build();

        callComposite.launch(this, remoteOptions);
    }

    private String fetchToken() {
        return "USER_ACCESS_TOKEN";
    }
}