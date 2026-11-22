package com.marketim3000.panel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.HttpAuthHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String BASE = "https://panel.marketim3000.online";
    private static final String PANEL_URL = BASE + "/sistem-paneli";
    private static final int DOSYA_SEC = 1001;

    private WebView web;
    private ProgressBar progress;
    private LinearLayout hataKutu;
    private TextView hataMetin;
    private ValueCallback<Uri[]> dosyaCallback;
    private SharedPreferences ayarlar;
    private String aktifKullanici = "";
    private String aktifSifre = "";
    private long sonGeri = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ayarlar = getSharedPreferences("sp_ayarlar", MODE_PRIVATE);
        aktifKullanici = ayarlar.getString("kullanici", "");
        aktifSifre = ayarlar.getString("sifre", "");

        LinearLayout kok = new LinearLayout(this);
        kok.setOrientation(LinearLayout.VERTICAL);
        kok.setBackgroundColor(Color.parseColor("#0b1120"));

        LinearLayout ust = new LinearLayout(this);
        ust.setOrientation(LinearLayout.HORIZONTAL);
        ust.setBackgroundColor(Color.parseColor("#0f172a"));
        ust.setPadding(10, 12, 10, 12);

        TextView baslik = new TextView(this);
        baslik.setText("Sistem Paneli");
        baslik.setTextColor(Color.parseColor("#e2e8f0"));
        baslik.setTextSize(15);
        baslik.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ust.addView(baslik, bp);

        ust.addView(dugme("Yenile", v -> web.reload()));
        ust.addView(dugme("Ana", v -> js("yukle('/')")));
        ust.addView(dugme("Üst", v -> ustKlasor()));
        ust.addView(dugme("Yükle", v -> js("document.getElementById('dosyaSecici').click()")));
        ust.addView(dugme("Menü", v -> menuAc()));
        kok.addView(ust, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        kok.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 6));

        hataKutu = new LinearLayout(this);
        hataKutu.setOrientation(LinearLayout.VERTICAL);
        hataKutu.setGravity(Gravity.CENTER);
        hataKutu.setPadding(24, 24, 24, 24);
        hataMetin = new TextView(this);
        hataMetin.setTextColor(Color.parseColor("#f87171"));
        hataMetin.setTextSize(14);
        hataMetin.setGravity(Gravity.CENTER);
        hataKutu.addView(hataMetin);
        hataKutu.addView(dugme("Tekrar Dene", v -> gir()));
        hataKutu.setVisibility(View.GONE);
        kok.addView(hataKutu, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0b1120"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setLoadsImagesAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                if (isFinishing()) {
                    handler.cancel();
                    return;
                }
                if (ayarlar.getBoolean("hatirla", false) && !aktifKullanici.isEmpty() && !aktifSifre.isEmpty()) {
                    handler.proceed(aktifKullanici, aktifSifre);
                    return;
                }
                final EditText ku = new EditText(MainActivity.this);
                ku.setHint("Kullanıcı adı");
                ku.setText(aktifKullanici);
                final EditText sh = new EditText(MainActivity.this);
                sh.setHint("Şifre");
                sh.setText(aktifSifre);
                sh.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                final CheckBox hatirla = new CheckBox(MainActivity.this);
                hatirla.setText("Bilgileri hatırla");
                hatirla.setChecked(ayarlar.getBoolean("hatirla", false));

                LinearLayout l = new LinearLayout(MainActivity.this);
                l.setOrientation(LinearLayout.VERTICAL);
                l.setPadding(24, 20, 24, 10);
                l.addView(ku, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                l.addView(sh, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                l.addView(hatirla, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Sistem Paneli Girişi")
                        .setView(l)
                        .setPositiveButton("Giriş", (d, w) -> {
                            aktifKullanici = ku.getText().toString();
                            aktifSifre = sh.getText().toString();
                            SharedPreferences.Editor e = ayarlar.edit();
                            e.putString("kullanici", aktifKullanici);
                            e.putString("sifre", aktifSifre);
                            e.putBoolean("hatirla", hatirla.isChecked());
                            e.apply();
                            handler.proceed(aktifKullanici, aktifSifre);
                        })
                        .setNegativeButton("İptal", (d, w) -> handler.cancel())
                        .show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest istek) {
                String url = istek.getUrl().toString();
                if (url.startsWith(BASE) || url.startsWith("https://marketim3000.online")) {
                    if (url.contains("/api/sistem-paneli/indir")) {
                        dosyayiIndir(url);
                        return true;
                    }
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Başlatılamadı", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                hataKutu.setVisibility(View.GONE);
                web.setVisibility(View.VISIBLE);
                js("(function(){var s=document.createElement('style');s.textContent='@media(max-width:760px){header{padding:10px 12px}header h1{font-size:14px}.ana{padding:10px}.tablo td,.tablo th{padding:7px 6px;font-size:12px}.araclar input[type=text]{min-width:120px;flex:1}}';document.head.appendChild(s);})()");
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest istek, WebResourceError hata) {
                if (istek.isForMainFrame()) {
                    hataMetin.setText("Bağlantı kurulamadı: " + hata.getDescription());
                    hataKutu.setVisibility(View.VISIBLE);
                    web.setVisibility(View.GONE);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int yeni) {
                progress.setProgress(yeni);
                progress.setVisibility(yeni >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                dosyaCallback = callback;
                Intent secici = new Intent(Intent.ACTION_GET_CONTENT);
                secici.addCategory(Intent.CATEGORY_OPENABLE);
                secici.setType("*/*");
                secici.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(Intent.createChooser(secici, "Dosya seç"), DOSYA_SEC);
                } catch (Exception e) {
                    callback.onReceiveValue(null);
                    dosyaCallback = null;
                    return false;
                }
                return true;
            }
        });

        kok.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(kok);
        kisayollariKur();
        gir();
    }

    @Override
    protected void onActivityResult(int istekKodu, int sonucKodu, Intent veri) {
        super.onActivityResult(istekKodu, sonucKodu, veri);
        if (istekKodu != DOSYA_SEC) return;
        Uri[] secilenler = null;
        if (sonucKodu == RESULT_OK && veri != null) {
            if (veri.getClipData() != null) {
                int adet = veri.getClipData().getItemCount();
                secilenler = new Uri[adet];
                for (int i = 0; i < adet; i++) {
                    secilenler[i] = veri.getClipData().getItemAt(i).getUri();
                }
            } else if (veri.getData() != null) {
                secilenler = new Uri[]{veri.getData()};
            }
            if (secilenler != null) {
                for (Uri u : secilenler) {
                    grantUriPermission(getPackageName(), u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
        }
        if (dosyaCallback != null) dosyaCallback.onReceiveValue(secilenler);
        dosyaCallback = null;
    }

    private void gir() {
        if (!agVarMi()) {
            hataMetin.setText("İnternet bağlantısı yok");
            hataKutu.setVisibility(View.VISIBLE);
            web.setVisibility(View.GONE);
            return;
        }
        hataKutu.setVisibility(View.GONE);
        web.setVisibility(View.VISIBLE);
        String url = PANEL_URL;
        String klasor = getIntent().getStringExtra("yol");
        if (klasor != null && !klasor.isEmpty()) {
            url += "?yol=" + Uri.encode(klasor);
        }
        web.loadUrl(url);
    }

    private void js(String kod) {
        web.evaluateJavascript(kod, null);
    }

    private void ustKlasor() {
        js("fetch('/api/sistem-paneli/liste?yol='+encodeURIComponent(simdiki),{credentials:'same-origin'})"
                + ".then(function(r){return r.json()}).then(function(d){yukle(d.ust)})");
    }

    private void dosyayiIndir(String url) {
        String ad = "sistem-dosya";
        try {
            String yol = Uri.parse(url).getQueryParameter("yol");
            if (yol != null && yol.lastIndexOf('/') >= 0) ad = yol.substring(yol.lastIndexOf('/') + 1);
        } catch (Exception e) {
        }
        DownloadManager.Request istek = new DownloadManager.Request(Uri.parse(url));
        istek.setTitle(ad);
        istek.setDescription("Sistem panelinden indiriliyor");
        istek.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        istek.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, ad);
        if (!aktifKullanici.isEmpty() && !aktifSifre.isEmpty()) {
            String kimlik = aktifKullanici + ":" + aktifSifre;
            istek.addRequestHeader("Authorization", "Basic " + Base64.encodeToString(kimlik.getBytes(), Base64.NO_WRAP));
        }
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(istek);
            Toast.makeText(this, "İndirme başladı: " + ad, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "İndirme başlatılamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean agVarMi() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    private Button dugme(String metin, View.OnClickListener dinleyici) {
        Button b = new Button(this);
        b.setText(metin);
        b.setTextSize(11);
        b.setTextColor(Color.parseColor("#e2e8f0"));
        b.setBackgroundColor(Color.parseColor("#243451"));
        b.setPadding(8, 4, 8, 4);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setOnClickListener(dinleyici);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(4, 0, 4, 0);
        b.setLayoutParams(p);
        return b;
    }

    private void menuAc() {
        String[] secenekler = {"Kimlik bilgilerini sıfırla", "İndirilenler", "Hakkında"};
        new AlertDialog.Builder(this)
                .setTitle("Seçenekler")
                .setItems(secenekler, (d, i) -> {
                    if (i == 0) {
                        ayarlar.edit().clear().apply();
                        aktifKullanici = "";
                        aktifSifre = "";
                        web.clearCache(true);
                        web.clearFormData();
                        web.reload();
                        Toast.makeText(this, "Kimlik bilgileri sıfırlandı", Toast.LENGTH_SHORT).show();
                    } else if (i == 1) {
                        try {
                            startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                        } catch (Exception e) {
                            Toast.makeText(this, "İndirilenler açılamadı", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Marketim Sistem Paneli v2.0")
                                .setMessage("Sunucu dosya yöneticisi.\n" + PANEL_URL)
                                .setPositiveButton("Tamam", null)
                                .show();
                    }
                })
                .show();
    }

    private void kisayollariKur() {
        if (Build.VERSION.SDK_INT < 25) return;
        ShortcutManager sm = getSystemService(ShortcutManager.class);
        if (sm == null) return;
        String[][] tanimlar = {
                {"kok", "Kök dizin", "/"},
                {"log", "Loglar", "/var/log"},
                {"code", "Panel kodu", "/home/userland"},
                {"drive", "Bulut sürücü", "/home/userland/drive_dosyalarim"}
        };
        List<ShortcutInfo> kisayollar = new ArrayList<>();
        for (String[] t : tanimlar) {
            Intent i = new Intent(this, MainActivity.class);
            i.setAction(Intent.ACTION_VIEW);
            i.putExtra("yol", t[2]);
            ShortcutInfo si = new ShortcutInfo.Builder(this, t[0])
                    .setShortLabel(t[1])
                    .setLongLabel(t[1])
                    .setIcon(Icon.createWithResource(this, android.R.drawable.ic_menu_manage))
                    .setIntent(i)
                    .build();
            kisayollar.add(si);
        }
        try {
            sm.setDynamicShortcuts(kisayollar);
        } catch (Exception e) {
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
            return;
        }
        if (System.currentTimeMillis() - sonGeri < 2000) {
            super.onBackPressed();
            return;
        }
        sonGeri = System.currentTimeMillis();
        Toast.makeText(this, "Çıkmak için tekrar basın", Toast.LENGTH_SHORT).show();
    }
}
