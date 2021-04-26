package com.example.videocallingquickstart;

import com.azure.android.communication.calling.DeviceManager;
import com.azure.android.communication.calling.IncomingCall;
import com.azure.android.communication.calling.VideoDeviceInfo;
import com.azure.android.communication.calling.CallAgent;

public class DataManager {
    private static CallAgent callAgent;
    private static IncomingCall incomingCall;
    private static DeviceManager deviceManager;
    private static VideoDeviceInfo defaultCamera;
    private static String deviceRegistrationToken;

    private DataManager() { }

    public static void setCallAgent(CallAgent callAgent) {
        DataManager.callAgent = callAgent;
    }

    public static void setDeviceManager(DeviceManager deviceManager) {
        DataManager.deviceManager = deviceManager;
    }

    public static void setDefaultCamera(VideoDeviceInfo camera) {
        defaultCamera = camera;
    }

    public static void setDeviceRegistrationToken(String deviceRegistrationToken) {
        DataManager.deviceRegistrationToken = deviceRegistrationToken;
    }

    public static void setIncomingCall(IncomingCall incomingCall) {
        DataManager.incomingCall = incomingCall;
    }

    public static CallAgent getCallAgent() {
        return callAgent;
    }

    public static DeviceManager getDeviceManager() {
        return deviceManager;
    }

    public static VideoDeviceInfo getDefaultCamera() {
        return defaultCamera;
    }

    public static String getDeviceRegistrationToken() {
        return deviceRegistrationToken;
    }

    public static IncomingCall getIncomingCall() { return incomingCall; }
}