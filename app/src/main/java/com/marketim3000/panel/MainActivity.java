package com.marketim3000.panel;

import android.app.Activity;
import android.app.AlertDialog;
import android.webkit.HttpAuthHandler;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedHttpAuthRequest(WebView view, final HttpAuthHandler handler, String host, String realm) {
                final EditText kullanici = new EditText(MainActivity.this);
                kullanici.setHint("Kullanıcı adı");
                final EditText sifre = new EditText(MainActivity.this);
                sifre.setHint("Şifre");
                sifre.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                int pad = 24;
                layout.setPadding(pad, pad, pad, pad);
                layout.addView(kullanici, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.addView(sifre, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Marketim Panel Girişi")
                        .setView(layout)
                        .setPositiveButton("Giriş", (dialog, which) -> handler.proceed(
                                kullanici.getText().toString(), sifre.getText().toString()))
                        .setNegativeButton("İptal", (dialog, which) -> handler.cancel())
                        .show();
            }
        });

        webView.loadUrl("https://panel.marketim3000.online/");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
