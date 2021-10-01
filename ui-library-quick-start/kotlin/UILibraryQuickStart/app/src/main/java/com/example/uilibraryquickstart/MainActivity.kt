package com.example.uilibraryquickstart

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.azure.android.communication.common.CommunicationTokenCredential
import com.azure.android.communication.common.CommunicationTokenRefreshOptions
import com.azure.android.communication.toolkit.CallingCompositeBuilder
import com.azure.android.communication.toolkit.CallingComposite
import com.azure.android.communication.toolkit.GroupCallParameters
import java.util.UUID

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startCallingCompositeButton = findViewById<Button>(R.id.startUILibraryButton)

        startCallingCompositeButton.setOnClickListener { l: View? -> startCallingComposite() }
    }

    private fun startCallingComposite() {
        val callingComposite: CallingComposite = CallingCompositeBuilder().build()
        val communicationTokenRefreshOptions = CommunicationTokenRefreshOptions(
            { fetchToken() }, true)
        val communicationTokenCredential =
            CommunicationTokenCredential(communicationTokenRefreshOptions)
        val parameters = GroupCallParameters(this,
            communicationTokenCredential,
            "DISPLAY_NAME",
            UUID.fromString("GROUP_CALL_ID"))
        callingComposite.startExperience(parameters)
    }

    private fun fetchToken(): String? {
        return "USER_ACCESS_TOKEN"
    }
}