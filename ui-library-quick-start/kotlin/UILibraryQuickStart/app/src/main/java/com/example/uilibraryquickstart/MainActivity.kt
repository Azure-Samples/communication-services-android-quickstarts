package com.example.uilibraryquickstart

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.azure.android.communication.common.CommunicationTokenCredential
import com.azure.android.communication.common.CommunicationTokenRefreshOptions
import com.azure.android.communication.ui.calling.CallComposite
import com.azure.android.communication.ui.calling.CallCompositeBuilder
import com.azure.android.communication.ui.calling.models.CallCompositeGroupCallLocator
import com.azure.android.communication.ui.calling.models.CallCompositeJoinLocator
import com.azure.android.communication.ui.calling.models.CallCompositeRemoteOptions
import java.util.UUID

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton: Button = findViewById(R.id.startButton)
        startButton.setOnClickListener { l -> startCallComposite() }
    }

    private fun startCallComposite() {
        val communicationTokenRefreshOptions = CommunicationTokenRefreshOptions({ fetchToken() }, true)
        val communicationTokenCredential = CommunicationTokenCredential(communicationTokenRefreshOptions)

        val locator: CallCompositeJoinLocator = CallCompositeGroupCallLocator(UUID.fromString("GROUP_CALL_ID"))
        val remoteOptions = CallCompositeRemoteOptions(locator, communicationTokenCredential, "DISPLAY_NAME")

        /**
        // Optional parameter - localOptions
        //    - to customize participant view data such as avatar image, scale type and display name
        //    - and to customize navigation bar's title and subtitle
        val participantViewData: CallCompositeParticipantViewData = CallCompositeParticipantViewData()
            .setAvatarBitmap((Bitmap) avatarBitmap)
            .setScaleType((ImageView.ScaleType) scaleType)
            .setDisplayName((String) displayName)

        val setupScreenViewData: CallCompositeSetupScreenViewData = CallCompositeSetupScreenViewData()
            .setTitle((String) title)
            .setSubtitle((String) subTitle)

        val localOptions: CallCompositeLocalOptions = CallCompositeLocalOptions()
            .setParticipantViewData(participantViewData)
            .setSetupScreenViewData(setupScreenViewData)

        callComposite.launch(callLauncherActivity, remoteOptions, localOptions);
        */

        val callComposite: CallComposite = CallCompositeBuilder().build()

        callComposite.launch(this, remoteOptions)
    }

    private fun fetchToken(): String? {
        return "USER_ACCESS_TOKEN"
    }
}