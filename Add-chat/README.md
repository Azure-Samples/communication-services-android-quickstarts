---
page_type: sample
languages:
- java
products:
- azure
- azure-communication-services
---

# Quickstart: Add Chat to your App

For full instructions on how to build this code sample from scratch, look at [Quickstart: Add chat to your Android app](https://docs.microsoft.com/azure/communication-services/quickstarts/chat/get-started?pivots=programming-language-android)

## Prerequisites

To complete this tutorial, you’ll need the following prerequisites:

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F). 
- [Android Studio](https://developer.android.com/studio), for running your Android application.
- A deployed Communication Services resource. [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).
- Create two Communication Services Users and issue them a user access token [User Access Token](https://docs.microsoft.com/azure/communication-services/quickstarts/access-tokens). Be sure to set the scope to chat, and note the token string and the userId string. In this quickstart, we will create a thread with an initial participant and then add a second participant to the thread.

## Code Structure

- **./app/src/main/java/com/contoso/chatquickstart/MainActivity.java:** Contains core logic for chat SDK integration.
- **./app/src/main/res/layout/activity_main.xml:** Contains core UI for sample app.


## Before running sample code

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. `git clone https://github.com/Azure-Samples/Communication-Services-Android-Quickstarts.git` 
3. With the `endpoint` procured in pre-requisites, add it to the **./app/src/main/java/com/contoso/chatquickstart/MainActivity.java** file. Assign your end point in line 31:
```private String endpoint = "https://<resource>.communication.azure.com";```
4. With the `Communication Services Users` procured in pre-requisites, add it to the **./app/src/main/java/com/contoso/chatquickstart/MainActivity.java** file. Assign your user identities in line 32 and 33:
```private String firstUserId = "<first_user_id>";```
```private String secondUserId = "<second_user_id>";```
5. With the `Access Token` procured in pre-requisites, add it to the **./app/src/main/java/com/contoso/chatquickstart/MainActivity.java** file. Assign your access token in line 34:
```private String firstUserAccessToken = "<first_user_access_token>"```

## Run the sample

In Android Studio, hit the Run button to build and run the project. In the console, you can view the output from the code and the logger output from the ChatClient.