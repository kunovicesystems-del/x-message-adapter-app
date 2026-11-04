package com.antigravity.xadapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    public static final String NETLIFY_APP_URL = "https://boisterous-lily-7b599e.netlify.app";
    private static final int REQUEST_OVERLAY_PERM = 101;
    private static final String PREFS_NAME = "x_adapter_prefs";
    private static final String KEY_BUBBLE_ACTIVE = "bubble_active";

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private MaterialButton btnToggleBubble;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        btnToggleBubble = findViewById(R.id.btnToggleBubble);

        setupWebView();
        setupSwipeRefresh();
        setupBubbleControls();

        // Restore or load page
        if (savedInstanceState == null) {
            webView.loadUrl(NETLIFY_APP_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setSupportZoom(false);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefresh.setRefreshing(false);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> webView.reload());
    }

    private void setupBubbleControls() {
        updateToggleButtonState();

        btnToggleBubble.setOnClickListener(v -> {
            boolean isRunning = isBubbleServiceRunning();
            if (isRunning) {
                stopBubbleService();
            } else {
                requestOverlayPermissionAndStart();
            }
        });
    }

    private void updateToggleButtonState() {
        boolean isRunning = isBubbleServiceRunning();
        if (isRunning) {
            btnToggleBubble.setText(R.string.btn_toggle_bubble_off);
        } else {
            btnToggleBubble.setText(R.string.btn_toggle_bubble_on);
        }
    }

    private boolean isBubbleServiceRunning() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_BUBBLE_ACTIVE, false);
    }

    private void setBubbleServiceState(boolean active) {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_BUBBLE_ACTIVE, active).apply();
        updateToggleButtonState();
    }

    private void requestOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.perm_prompt_title)
                        .setMessage(R.string.perm_prompt_desc)
                        .setPositiveButton(R.string.perm_btn_grant, (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, REQUEST_OVERLAY_PERM);
                        })
                        .setNegativeButton(R.string.perm_btn_cancel, null)
                        .show();
                return;
            }
        }
        startBubbleService();
    }

    private void startBubbleService() {
        Intent intent = new Intent(this, FloatingBubbleService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        setBubbleServiceState(true);
        Toast.makeText(this, "✂️ Botón flotante activado sobre la pantalla", Toast.LENGTH_SHORT).show();
    }

    private void stopBubbleService() {
        Intent intent = new Intent(this, FloatingBubbleService.class);
        stopService(intent);
        setBubbleServiceState(false);
        Toast.makeText(this, "Botón flotante desactivado", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERM) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startBubbleService();
            } else {
                Toast.makeText(this, "Permiso no concedido.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateToggleButtonState();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
