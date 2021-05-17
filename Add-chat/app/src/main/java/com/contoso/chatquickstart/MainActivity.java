package com.contoso.chatquickstart;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import com.azure.android.communication.chat.*;
import com.azure.android.communication.chat.models.*;
import com.azure.android.communication.common.*;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.jakewharton.threetenabp.AndroidThreeTen;
import org.threeten.bp.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;

import com.azure.android.core.credential.AccessToken;
import com.azure.android.core.http.okhttp.OkHttpAsyncClientProvider;
import com.azure.android.core.http.policy.BearerTokenAuthenticationPolicy;
import com.azure.android.core.http.policy.UserAgentPolicy;

import com.azure.android.communication.chat.signaling.chatevents.BaseEvent;
import com.azure.android.communication.chat.signaling.chatevents.ChatMessageReceivedEvent;
import com.azure.android.communication.chat.signaling.properties.ChatEventId;

import com.azure.android.core.rest.PagedResponse;
import com.azure.android.core.util.Context;

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
    private final String sdkVersion = "1.0.0-beta.8";
    private static final String APPLICATION_ID = "Chat Quickstart App";
    private static final String SDK_NAME = "azure-communication-com.azure.android.communication.chat";
    private static final String TAG = "Chat Quickstart App";

    private void log(String msg) {
        Log.i(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    void listParticipantsNextPage(ChatThreadAsyncClient chatThreadAsyncClient, String continuationToken, int pageNumber) {
        if (continuationToken != null) {
            try {
                PagedResponse<ChatParticipant> nextPageWithResponse = chatThreadAsyncClient.getParticipantsNextPageWithResponse(continuationToken, Context.NONE).get();
                for (ChatParticipant chatParticipant : nextPageWithResponse.getValue()) {
                    // You code to handle participant
                }

                listParticipantsNextPage(chatThreadAsyncClient, nextPageWithResponse.getContinuationToken(), ++pageNumber);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void listReadReceiptsNextPage(ChatThreadAsyncClient chatThreadAsyncClient, String continuationToken, int pageNumber) {
        if (continuationToken != null) {
            try {
                PagedResponse<ChatMessageReadReceipt> nextPageWithResponse =
                        chatThreadAsyncClient.getReadReceiptsNextPageWithResponse(continuationToken, Context.NONE).get();

                for (ChatMessageReadReceipt readReceipt : nextPageWithResponse.getValue()) {
                    // You code to handle readReceipt
                }

                listParticipantsNextPage(chatThreadAsyncClient, nextPageWithResponse.getContinuationToken(), ++pageNumber);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            AndroidThreeTen.init(this);

            // <CREATE A CHAT CLIENT>
            ChatAsyncClient chatAsyncClient = new ChatClientBuilder()
                    .endpoint(endpoint)
                    .credentialPolicy(new BearerTokenAuthenticationPolicy((request, callback) ->
                            callback.onSuccess(new AccessToken(firstUserAccessToken, OffsetDateTime.now().plusDays(1))), "chat"))
                    .addPolicy(new UserAgentPolicy(APPLICATION_ID, SDK_NAME, sdkVersion))
                    .httpClient(new OkHttpAsyncClientProvider().createInstance())
                    .realtimeNotificationParams(getApplicationContext(), firstUserAccessToken)
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
                    .credentialPolicy(new BearerTokenAuthenticationPolicy((request, callback) ->
                            callback.onSuccess(new AccessToken(firstUserAccessToken, OffsetDateTime.now().plusDays(1))), "chat"))
                    .addPolicy(new UserAgentPolicy(APPLICATION_ID, SDK_NAME, sdkVersion))
                    .httpClient(new OkHttpAsyncClientProvider().createInstance())
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
            chatAsyncClient.startRealtimeNotifications();

            // Register a listener for chatMessageReceived event
            chatAsyncClient.on(ChatEventId.chatMessageReceived, "chatMessageReceived", (BaseEvent payload) -> {
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

            PagedResponse<ChatParticipant> getParticipantsFirstPageWithResponse =
                    chatThreadAsyncClient.getParticipantsFirstPageWithResponse(listParticipantsOptions, Context.NONE).get();

            for (ChatParticipant chatParticipant : getParticipantsFirstPageWithResponse.getValue()) {
                // You code to handle participant
            }

            listParticipantsNextPage(chatThreadAsyncClient, getParticipantsFirstPageWithResponse.getContinuationToken(), 2);


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

            PagedResponse<ChatMessageReadReceipt> listReadReceiptsFirstPageWithResponse =
                    chatThreadAsyncClient.getReadReceiptsFirstPageWithResponse(listReadReceiptOptions, Context.NONE).get();

            for (ChatMessageReadReceipt readReceipt : listReadReceiptsFirstPageWithResponse.getValue()) {
                // You code to handle readReceipt
            }

            listReadReceiptsNextPage(chatThreadAsyncClient, listReadReceiptsFirstPageWithResponse.getContinuationToken(), 2);
            
        } catch (Exception e){
            System.out.println("Quickstart failed: " + e.getMessage());
        }
    }
}