package com.example.webviewquickstart;

import android.Manifest;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private WebViewWrapper webView;

    // Please set the defaultUrl
    private final String defaultUrl = null;

    private final long silentDurationInMs = 5000;
    private String lastToastMessage = null;
    private long lastToastMessageTime = 0;

    private void showToast(String toastMessage) {
        long currentTime = System.currentTimeMillis();
        if((toastMessage != null) && !toastMessage.isEmpty() &&
                (!toastMessage.equals(lastToastMessage) || (currentTime - lastToastMessageTime > silentDurationInMs))) {
            lastToastMessage = toastMessage;
            runOnUiThread(() -> Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show());
        }
        lastToastMessageTime = currentTime;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = new WebViewWrapper(this, findViewById(R.id.webView));
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if(defaultUrl != null) {
                webView.reload();
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        webView.setListener(new WebViewWrapper.WebViewWrapperListener() {
            @Override
            public void onPageFinished(String url) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        if(defaultUrl != null) {
            loadUrl(defaultUrl);
        }
    }

    /**
     * Callback for the result from requesting permissions.
     * We continue to use WebView if the user doesn't grant permissions.
     * A user can still join the call without using mic, camera
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        ArrayList<String> permissionsNotGranted = new ArrayList<>();
        for(int i = 0; i < grantResults.length; i++) {
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                switch (permissions[i]){
                    case Manifest.permission.RECORD_AUDIO:
                        permissionsNotGranted.add("Microphone");
                        break;
                    case Manifest.permission.CAMERA:
                        permissionsNotGranted.add("Camera");
                        break;
                }
            }
        }
        if (!permissionsNotGranted.isEmpty()) {
            showToast(String.format("%s permission(s) not granted. Some functions may not work.", String.join(",", permissionsNotGranted.toArray(new String[0]))));
        }
        if (requestCode == WebViewWrapper.requestCode) {
            webView.updatePermission();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    private void loadUrl(String url){
        webView.loadUrl(url);
        getSupportActionBar().setTitle(url);
        showToast(String.format("Loading: %s", url));
    }
}
