package jp.co.spaceconnect.smassoadvance;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SharedPreferences sharedPreferences;
    private int tapCount = 0;
    private static final String PREFS_NAME = "WebViewAppPrefs";
    private static final String KEY_SUPPORT_ID = "supportId";
    private static final String KEY_DEV = "dev";
    private static final String DEFAULT_SUPPORT_ID = "00000";
    private static final boolean DEFAULT_DEV = true;
    private Handler handler = new Handler();
    private Runnable resetTapCountRunnable = new Runnable() {
        @Override
        public void run() {
            tapCount = 0;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        webView = findViewById(R.id.webView);
        View hiddenButton = findViewById(R.id.hiddenButton);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupWebView();
        // 30秒待機してからURLを読み込む
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadUrlWithParams();
            }
        }, 10000); // 10000ミリ秒 = 10秒

        hiddenButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    tapCount++;
                    Log.d("HiddenButton", "Tap count: " + tapCount); // デバッグメッセージを追加
                    handler.removeCallbacks(resetTapCountRunnable);
                    handler.postDelayed(resetTapCountRunnable, 1000); // 1秒後にタップカウントをリセット
                    if (tapCount == 10) {
                        tapCount = 0;
                        showSettingsDialog();
                    }
                }
                return true;
            }
        });
    }

    private void setupWebView() {
        Log.d("WebView", "Setting up WebView");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false); // 自動メディア再生を許可
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); // JavaScriptによるウィンドウの自動生成を許可

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("WebView", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return true;
            }
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d("WebView", "onPermissionRequest called");
                Log.d("WebView", "onPermissionRequest: " + request.getResources().toString());
                for (String resource : request.getResources()) {
                    Log.d("WebView", "Requested resource: " + resource);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (request.getResources().length > 0) {
                            request.grant(request.getResources());
                            Log.d("WebView", "Permissions granted for: " + request.getResources().toString());
                        } else {
                            request.deny();
                            Log.d("WebView", "Permissions denied");
                        }
                    }
                });
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                Log.d("WebView", "onPermissionRequestCanceled");
            }

        });
        webView.setInitialScale(90); // Set the initial scale to 90%

        // Disable text selection
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        webView.setLongClickable(false);
    }

    private void loadUrlWithParams() {
        String supportId = sharedPreferences.getString(KEY_SUPPORT_ID, DEFAULT_SUPPORT_ID);
        boolean dev = sharedPreferences.getBoolean(KEY_DEV, DEFAULT_DEV);
        String url = "https://td77k978y9.execute-api.ap-northeast-1.amazonaws.com/?supportId=" + supportId;
        if (dev) {
            url += "&dev=true";
        }
        webView.loadUrl(url);
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText supportIdInput = new EditText(this);
        supportIdInput.setHint("管理ID");
        supportIdInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        supportIdInput.setText(sharedPreferences.getString(KEY_SUPPORT_ID, DEFAULT_SUPPORT_ID));
        layout.addView(supportIdInput);

        final Switch devSwitch = new Switch(this);
        devSwitch.setText("試験環境の時はスイッチをONにしてください。");
        devSwitch.setChecked(sharedPreferences.getBoolean(KEY_DEV, DEFAULT_DEV));
        layout.addView(devSwitch);

        builder.setView(layout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String supportId = supportIdInput.getText().toString();
                boolean dev = devSwitch.isChecked();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_SUPPORT_ID, supportId);
                editor.putBoolean(KEY_DEV, dev);
                editor.apply();

                String url = "https://td77k978y9.execute-api.ap-northeast-1.amazonaws.com/?supportId=" + supportId;
                if (dev) {
                    url += "&dev=true";
                }

                loadUrlWithParams();
                Toast.makeText(MainActivity.this, "Settings saved: " + url, Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", null);

        builder.setNeutralButton("Open Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
            }
        });

        builder.show();
    }
}