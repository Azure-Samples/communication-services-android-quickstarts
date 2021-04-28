---
page_type: sample
languages:
- java
products:
- azure
- azure-communication-services
---

# Quickstart: Add telephone calling to your Android app

For full instructions on how to build this code sample from scratch, look at [Quickstart: Add telephone calling to your Android app](https://docs.microsoft.com/azure/communication-services/quickstarts/voice-video-calling/pstn-call?pivots=platform-android)

## Prerequisites

To complete this tutorial, you’ll need the following prerequisites:

- An Azure account with an active subscription. [Create an account for free](https://azure.microsoft.com/free/?WT.mc_id=A261C142F). 
- [Android Studio](https://developer.android.com/studio), for running your Android application.
- A deployed Communication Services resource. [Create a Communication Services resource](https://docs.microsoft.com/azure/communication-services/quickstarts/create-communication-resource).
- A [User Access Token](https://docs.microsoft.com/azure/communication-services/quickstarts/access-tokens) for your Azure Communication Service.
- A phone number acquired in Communication Services resource. [how to get a phone number](https://docs.microsoft.com/azure/communication-services/quickstarts/telephony-sms/get-phone-number).
- Complete the quickstart for [getting started with adding calling to your application](https://docs.microsoft.com/azure/communication-services/quickstarts/voice-video-calling/getting-started-with-calling).
## Code Structure

- **./app/src/main/java/com/contoso/acsquickstart/MainActivity.java:** Contains core logic for calling SDK integration.
- **./app/src/main/res/layout/activity_main.xml:** Contains core UI for sample app.

## Object model

The following classes and interfaces used in the quickstart handle some of the major features of the Azure Communication Services Calling client library:

| Name                                  | Description                                                  |
| ------------------------------------- | ------------------------------------------------------------ |
| CallClient| The CallClient is the main entry point to the Calling client library.|
| CallAgent | The CallAgent is used to start and manage calls. |
| CommunicationTokenCredential  | The CommunicationTokenCredential  is used as the token credential to instantiate the CallAgent.|

## Before running sample code

1. Open an instance of PowerShell, Windows Terminal, Command Prompt or equivalent and navigate to the directory that you'd like to clone the sample to.
2. `git clone https://github.com/Azure-Samples/Communication-Services-Android-Quickstarts.git` 
3. With the `Access Token` procured in pre-requisites, add it to the **./app/src/main/java/com/contoso/acsquickstart/MainActivity.java** file. Assign your access token in line 30:
   ```private static final String UserToken = "<User_Access_Token>";```
3. With the `phone number` procured in pre-requisites, add it to the **./app/src/main/java/com/contoso/acsquickstart/MainActivity.java** file. Assign your ACS phone number in line 60:
   ```PhoneNumberIdentifier callerPhone = new PhoneNumberIdentifier("ACS Phone number");```

## Run the sample

Open the sample project using Android Studio and run the application.

![Final look and feel of the quick start app](../Media/quickstart-android-call-pstn.png)

The app can now be launched using the "Run App" button on the toolbar (Shift+F10). You can make an call to phone by providing a phone number in the added text field and clicking the CALL button
