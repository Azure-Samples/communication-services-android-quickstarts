package com.example.webviewquickstart;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Environment;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.Manifest;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class WebViewWrapper {
    private WebView webView;
    private Activity parentActivity;
    private ArrayList<PermissionRequest> permissionRequests = new ArrayList<>();
    private PermissionRequest currentPermissionRequest = null;
    public static final int requestCode = 2;

    public interface WebViewWrapperListener {
        public void onPageFinished(String url);
    }
    private WebViewWrapperListener listener = null;

    private void showToast(String toastMessage) {
        if((toastMessage != null) && !toastMessage.isEmpty()) {
            webView.post(() -> Toast.makeText(webView.getContext(), toastMessage, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Grant/Deny web page permission request based on current app permissions
     */
    private void updatePermission(PermissionRequest request) {
        String[] permissionsRequested = request.getResources();
        ArrayList<String> permissionsGranted = new ArrayList<>();
        for(String permission: permissionsRequested) {
            if(permission.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                if(ActivityCompat.checkSelfPermission(parentActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted.add(permission);
                }
            } else if(permission.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                if(ActivityCompat.checkSelfPermission(parentActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted.add(permission);
                }
            }
        }
        if(permissionsGranted.isEmpty()) {
            request.deny();
        } else {
            request.grant(permissionsGranted.toArray(new String[0]));
        }
    }

    /**
     * Request app permissions based on web page permission request.
     * If the permission is allowed already, we don't request that permission.
     * If all permissions requested by web page are allowed, we bypass the app permission request and grant the web page permission request immediately
     */
    private void requestPermission(PermissionRequest request) {
        if(currentPermissionRequest != null) {
            permissionRequests.add(request);
            return;
        }
        String[] permissionsRequested = request.getResources();
        ArrayList<String> permissionsGranted = new ArrayList<>();
        ArrayList<String> permissionsToAskFor = new ArrayList<>();
        for(String permission: permissionsRequested) {
            if(permission.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                if(ActivityCompat.checkSelfPermission(parentActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted.add(permission);
                } else {
                    permissionsToAskFor.add(Manifest.permission.RECORD_AUDIO);
                }
            } else if(permission.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                if(ActivityCompat.checkSelfPermission(parentActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted.add(permission);
                } else {
                    permissionsToAskFor.add(Manifest.permission.CAMERA);
                }
            }
        }
        if(!permissionsToAskFor.isEmpty()) {
            currentPermissionRequest = request;
            ActivityCompat.requestPermissions(parentActivity, permissionsToAskFor.toArray(new String[0]), requestCode);
        } else {
            request.grant(permissionsGranted.toArray(new String[0]));
        }
    }

    public void setListener(WebViewWrapperListener listener) {
        this.listener = listener;
    }

    /**
     * Update web page permission request based on the result of app permission request.
     */
    public void updatePermission() {
        webView.post(new Runnable() {
            @Override
            public void run() {
                if (currentPermissionRequest != null) {
                    updatePermission(currentPermissionRequest);
                    currentPermissionRequest = null;
                }
                if (!permissionRequests.isEmpty()) {
                    requestPermission(permissionRequests.remove(0));
                }
            }
        });
    }

    class CustomWebViewClient extends WebViewClient {
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            StringBuilder promptMessageBuilder = new StringBuilder();
            promptMessageBuilder.append("SSL Error:");
            promptMessageBuilder.append(error.toString());
            promptMessageBuilder.append(" Do you want to continue?");

            AlertDialog.Builder builder = new AlertDialog.Builder(webView.getContext());
            builder.setMessage(promptMessageBuilder.toString()).setCancelable(false).setPositiveButton(R.string.yes_txt, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id){
                    handler.proceed();
                }
            }).setNegativeButton(R.string.no_txt, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    handler.cancel();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            if (listener != null) {
                listener.onPageFinished(url);
            }
        }
    }

    class CustomWebChromeClient extends WebChromeClient {
        /**
         * The web content is requesting permission to access the specified resource
         * Only process audio/video capture permission here.
         */
        @Override
        public void onPermissionRequest(PermissionRequest request) {
            requestPermission(request);
        }
    }

    class CustomDownloadListener implements DownloadListener {
        /**
         * Notify when a resource should be downloaded
         * Only handle plain text data url to support communication-services-web-calling-tutorial sample page log download.
         * It will save files to app private Download folder
         */
        @Override
        public void onDownloadStart(String url, String userAgent,
                                    String contentDisposition, String mimeType,
                                    long contentLength) {
            try {
                String dataUrlPrefix = "data:text/plain;charset=utf-8,";
                if(url.startsWith(dataUrlPrefix)){
                    new Thread(new Runnable() {
                        public void run() {
                            String toastMessage = "";
                            try {
                                File destDir = webView.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                                String fileName = String.format("log-%s.txt", df.format(new Date()));
                                File file = new File(destDir, fileName);
                                String str = Uri.decode(url.substring(dataUrlPrefix.length()));
                                FileOutputStream stream = new FileOutputStream(file);
                                try {
                                    stream.write(str.getBytes(StandardCharsets.UTF_8));
                                    toastMessage = String.format("%s is saved in app private Download folder", fileName);
                                } finally {
                                    stream.close();
                                }
                            } catch (Exception e) {
                                toastMessage = String.format("Download error: %s", e.toString());
                            }
                            showToast(toastMessage);
                        }
                    }).start();
                } else {
                    showToast("Download error: download type is not supported");
                }
            } catch (Exception e) {
                showToast(String.format("Download error: %s", e.toString()));
            }
        }
    }

    /**
     * WebViewWrapper constructor
     * configure WebView and register necessary handler
     * debugging with chrome://inspect is enabled
     */
    public WebViewWrapper(Activity parentActivity, WebView view) {
        this.parentActivity = parentActivity;
        webView = view;
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebContentsDebuggingEnabled(true);
        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());
        webView.setDownloadListener(new CustomDownloadListener());
    }

    public void loadUrl(String url) {
        webView.loadUrl(url);
    }

    public void reload(){
        webView.reload();
    }

    public void destroy(){
        webView.destroy();
    }
}
