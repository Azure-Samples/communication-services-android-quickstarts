package com.contoso.chatquickstart;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import com.azure.android.communication.chat.*;
import com.azure.android.communication.chat.models.*;
import com.azure.android.communication.common.*;

import android.util.Log;
import android.widget.Toast;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.util.ArrayList;
import java.util.List;

import com.azure.android.core.http.policy.UserAgentPolicy;

import com.azure.android.core.rest.util.paging.PagedAsyncStream;
import com.azure.android.core.util.RequestContext;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private String endpoint = "https://<resource>.communication.azure.com";
    private String firstUserId = "<first_user_id>";
    private String secondUserId = "<second_user_id>";
    private String firstUserAccessToken = "<first_user_access_token>";
    private String threadId = "";
    private String chatMessageId = "";
    private final String sdkVersion = "1.0.0";
    private static final String APPLICATION_ID = "Chat Quickstart App";
    private static final String SDK_NAME = "azure-communication-com.azure.android.communication.chat";
    private static final String TAG = "Chat Quickstart App";

    private void log(String msg) {
        Log.i(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            AndroidThreeTen.init(this);

            // <CREATE A CHAT CLIENT>
            ChatAsyncClient chatAsyncClient = new ChatClientBuilder()
                    .endpoint(endpoint)
                    .credential(new CommunicationTokenCredential(firstUserAccessToken))
                    .addPolicy(new UserAgentPolicy(APPLICATION_ID, SDK_NAME, sdkVersion))
                    .buildAsyncClient();

            // <CREATE A CHAT THREAD>
            // A list of ChatParticipant to start the thread with.
            List<ChatParticipant> participants = new ArrayList<>();
            // The display name for the thread participant.
            String displayName = "initial participant";
            participants.add(new ChatParticipant()
                    .setCommunicationIdentifier(new CommunicationUserIdentifier(firstUserId))
                    .setDisplayName(displayName));

            // The topic for the thread.
            final String topic = "General";
            // Optional, set a repeat request ID.
            final String repeatabilityRequestID = "";
            // Options to pass to the create method.
            CreateChatThreadOptions createChatThreadOptions = new CreateChatThreadOptions()
                    .setTopic(topic)
                    .setParticipants(participants)
                    .setIdempotencyToken(repeatabilityRequestID);

            CreateChatThreadResult createChatThreadResult =
                    chatAsyncClient.createChatThread(createChatThreadOptions).get();
            ChatThreadProperties chatThreadProperties = createChatThreadResult.getChatThreadProperties();
            threadId = chatThreadProperties.getId();

            // <CREATE A CHAT THREAD CLIENT>

            ChatThreadAsyncClient chatThreadAsyncClient = new ChatThreadClientBuilder()
                    .endpoint(endpoint)
                    .credential(new CommunicationTokenCredential(firstUserAccessToken))
                    .addPolicy(new UserAgentPolicy(APPLICATION_ID, SDK_NAME, sdkVersion))
                    .chatThreadId(threadId)
                    .buildAsyncClient();

            // <SEND A MESSAGE>
            // The chat message content, required.
            final String content = "Test message 1";
            // The display name of the sender, if null (i.e. not specified), an empty name will be set.
            final String senderDisplayName = "An important person";
            SendChatMessageOptions chatMessageOptions = new SendChatMessageOptions()
                    .setType(ChatMessageType.TEXT)
                    .setContent(content)
                    .setSenderDisplayName(senderDisplayName);

            // A string is the response returned from sending a message, it is an id, which is the unique ID of the message.
            chatMessageId = chatThreadAsyncClient.sendMessage(chatMessageOptions).get().getId();
            
            // <RECEIVE CHAT MESSAGES>

            // Start real time notification
            chatAsyncClient.startRealtimeNotifications(firstUserAccessToken, getApplicationContext());

            // Register a listener for chatMessageReceived event
            chatAsyncClient.addEventHandler(ChatEventType.CHAT_MESSAGE_RECEIVED, (ChatEvent payload) -> {
                ChatMessageReceivedEvent chatMessageReceivedEvent = (ChatMessageReceivedEvent) payload;
                // You code to handle chatMessageReceived event

            });

            // <ADD A USER>
            // The display name for the thread participant.
            String secondUserDisplayName = "a new participant";
            ChatParticipant participant = new ChatParticipant()
                    .setCommunicationIdentifier(new CommunicationUserIdentifier(secondUserId))
                    .setDisplayName(secondUserDisplayName);

            chatThreadAsyncClient.addParticipant(participant);
            
            // <LIST USERS>
            // The maximum number of participants to be returned per page, optional.
            int maxPageSize = 10;

            // Skips participants up to a specified position in response.
            int skip = 0;

            // Options to pass to the list method.
            ListParticipantsOptions listParticipantsOptions = new ListParticipantsOptions()
                    .setMaxPageSize(maxPageSize)
                    .setSkip(skip);

            PagedAsyncStream<ChatParticipant> participantsPagedAsyncStream =
                    chatThreadAsyncClient.listParticipants(listParticipantsOptions, RequestContext.NONE);

            participantsPagedAsyncStream.forEach(chatParticipant -> {
                // You code to handle participant
            });

            // <REMOVE A USER>
            // Using the unique ID of the participant.
            chatThreadAsyncClient.removeParticipant(new CommunicationUserIdentifier(secondUserId)).get();

            // <<SEND A TYPING NOTIFICATION>>
            chatThreadAsyncClient.sendTypingNotification().get();
			
            // <<SEND A READ RECEIPT>>
            chatThreadAsyncClient.sendReadReceipt(chatMessageId).get();
			
            // <<LIST READ RECEIPTS>>
            // The maximum number of participants to be returned per page, optional.
            maxPageSize = 10;
            // Skips participants up to a specified position in response.
            skip = 0;
            // Options to pass to the list method.
            ListReadReceiptOptions listReadReceiptOptions = new ListReadReceiptOptions()
                    .setMaxPageSize(maxPageSize)
                    .setSkip(skip);

            PagedAsyncStream<ChatMessageReadReceipt> readReceiptsPagedAsyncStream =
                    chatThreadAsyncClient.listReadReceipts(listReadReceiptOptions, RequestContext.NONE);

            readReceiptsPagedAsyncStream.forEach(readReceipt -> {
                // You code to handle readReceipt
            });
            
        } catch (Exception e){
            System.out.println("Quickstart failed: " + e.getMessage());
        }
    }
}