package com.example.uilibraryquickstart.chat

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.azure.android.communication.common.CommunicationTokenCredential
import com.azure.android.communication.common.CommunicationTokenRefreshOptions
import com.azure.android.communication.ui.chat.ChatAdapter
import com.azure.android.communication.ui.chat.ChatAdapterBuilder
import com.azure.android.communication.ui.chat.presentation.ChatCompositeView

class MainActivity : AppCompatActivity() {
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val startButton = findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener { l: View? ->
            val communicationTokenRefreshOptions =
                CommunicationTokenRefreshOptions(
                    { accessToken }, true
                )
            val communicationTokenCredential =
                CommunicationTokenCredential(communicationTokenRefreshOptions)
            chatAdapter = ChatAdapterBuilder()
                .endpointUrl(endpoint)
                .communicationTokenCredential(communicationTokenCredential)
                .identity(acsIdentity)
                .displayName(displayName)
                .build()
            try {
                chatAdapter.connect(this@MainActivity, threadId).get()
                val chatView: View = ChatCompositeView(this@MainActivity, chatAdapter)
                addContentView(
                    chatView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            } catch (e: Exception) {
                var messageCause: String? = "Unknown error"
                if (e.cause != null && e.cause!!.message != null) {
                    messageCause = e.cause!!.message
                }
                showAlert(messageCause)
            }
        }
    }

    /**
     *
     * @return String endpoint URL from ACS Admin UI, "https://example.domain.com/"
     */
    private val endpoint: String?
        get() = "https://example.domain.com/"

    /**
     *
     * @return String identity of the user joining the chat
     * Looks like "8:acs:a6aada1f-0b1e-47ac-866a-91aae00a1c01_00000015-45ee-bad7-0ea8-923e0d008a89"
     */
    private val acsIdentity: String?
        get() = ""

    /**
     *
     * @return String display name of the user joining the chat
     */
    private val displayName: String?
        get() = ""

    /**
     *
     * @return String secure ACS access token for the current user
     */
    private val accessToken: String?
        get() = ""

    /**
     *
     * @return String id of ACS chat thread to join
     * Looks like "19:AVNnEll25N4KoNtKolnUAhAMu8ntI_Ra03saj0Za0r01@thread.v2"
     */
    private val threadId: String?
        get() = ""

    fun showAlert(message: String?) {
        runOnUiThread {
            AlertDialog.Builder(this@MainActivity)
                .setMessage(message)
                .setTitle("Alert")
                .setPositiveButton(
                    "OK"
                ) { _, i -> }
                .show()
        }
    }
}