package com.microsoft.acs.calling.rawmediaaccess;

public class ScreenCaptureListenerBridge {

    private static IScreenCaptureListener listener;
    private final static Object locker;

    static {

        locker = new Object();
    }

    private ScreenCaptureListenerBridge() {
    }

    public static void SetListener(IScreenCaptureListener listener) {

        synchronized (locker) {

            ScreenCaptureListenerBridge.listener = listener;
        }
    }

    public static IScreenCaptureListener GetListener() {

        synchronized (locker) {

            return listener;
        }
    }
}