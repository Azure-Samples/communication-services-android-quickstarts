package com.contoso.roomscallquickstart

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.azure.android.communication.calling.Call
import com.azure.android.communication.calling.CallAgent
import com.azure.android.communication.calling.CallClient
import com.azure.android.communication.calling.HangUpOptions
import com.azure.android.communication.calling.JoinCallOptions
import com.azure.android.communication.calling.RoomCallLocator
import com.azure.android.communication.common.CommunicationTokenCredential
import java.util.concurrent.ExecutionException

class MainActivity : AppCompatActivity() {
    private val allPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE
    )

    private val userToken = "<ACS_USER_TOKEN>"
    private lateinit var callAgent: CallAgent
    private var call: Call? = null

    private lateinit var roleTextView: TextView
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getAllPermissions()
        createCallAgent()

        val callButton: Button = findViewById(R.id.call_button)
        callButton.setOnClickListener { startCall() }

        val hangupButton: Button = findViewById(R.id.hangup_button)
        hangupButton.setOnClickListener { endCall() }

        roleTextView = findViewById(R.id.text_role)
        statusView = findViewById(R.id.text_call_status)

        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    /**
     * Start a call
     */
    private fun startCall() {
        if (userToken.startsWith("<")) {
            Toast.makeText(this, "Please enter token in source code", Toast.LENGTH_SHORT).show()
            return
        }

        val roomIdView: EditText = findViewById(R.id.room_id)
        val roomId = roomIdView.text.toString()
        if (roomId.isEmpty()) {
            Toast.makeText(this, "Please enter room ID", Toast.LENGTH_SHORT).show()
            return
        }

        val joinCallOptions = JoinCallOptions()

        val roomCallLocator = RoomCallLocator(roomId)
        call = callAgent.join(applicationContext, roomCallLocator, joinCallOptions)
        
        call?.addOnStateChangedListener { setCallStatus(call?.state.toString()) }

        call?.addOnRoleChangedListener { setRoleText(call?.callParticipantRole.toString()) }
    }

    /**
     * Ends the call previously started
     */
    private fun endCall() {
        try {
            call?.hangUp(HangUpOptions())?.get()
        } catch (e: ExecutionException) {
            Toast.makeText(this, "Unable to hang up call", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Create the call callAgent
     */
    private fun createCallAgent() {
            try {
                val credential = CommunicationTokenCredential(userToken)
                callAgent = CallClient().createCallAgent(applicationContext, credential).get()
            } catch (ex: Exception) {
                Toast.makeText(
                    applicationContext,
                    "Failed to create call callAgent.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**
     * Request each required permission if the app doesn't already have it.
     */
    private fun getAllPermissions() {
        val permissionsToAskFor = mutableListOf<String>()
        for (permission in allPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToAskFor.add(permission)
            }
        }
        if (permissionsToAskFor.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToAskFor.toTypedArray(), 1)
        }
    }

    /**
     * Ensure all permissions were granted, otherwise inform the user permissions are missing.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var allPermissionsGranted = true
        for (result in grantResults) {
            allPermissionsGranted = allPermissionsGranted && (result == PackageManager.PERMISSION_GRANTED)
        }
        if (!allPermissionsGranted) {
            Toast.makeText(this, "All permissions are needed to make the call.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setCallStatus(status: String?) {
        runOnUiThread {
            statusView.text = "Call Status: $status"
        }
    }
    @SuppressLint("SetTextI18n")
    private fun setRoleText(role: String?) {
        runOnUiThread {
            roleTextView.text = "Role: $role"
        }
    }
}
