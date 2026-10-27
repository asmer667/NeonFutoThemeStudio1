package com.futo.themeeditor;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView webView;
    private android.webkit.ValueCallback<android.net.Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        // إعدادات فتح نافذة اختيار الملفات
        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, android.webkit.ValueCallback<android.net.Uri[]> filePathCallback, android.webkit.WebChromeClient.FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                android.content.Intent intent = fileChooserParams.createIntent();
                try {
                    MainActivity.this.startActivityForResult(intent, 1);
                } catch (android.content.ActivityNotFoundException e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }
        });
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");
        // تفعيل تصحيح الأخطاء
        android.webkit.WebView.setWebContentsDebuggingEnabled(true);

        // طباعة أخطاء الكونسول
        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage cm) {
                android.util.Log.e("FUTODEBUG", cm.message() + " -- Line " + cm.lineNumber());
                return true;
            }
        });

        // تحميل الصفحة الرئيسية
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void saveZip(String base64Data, String fileName) {
            try {
                byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        os.write(bytes);
                        os.close();
                    }
                } else {
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(dir, fileName);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(bytes);
                    fos.close();
                }
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "تم الحفظ في Downloads: " + fileName, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "فشل الحفظ: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }
    }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        if (requestCode == 1) {
            if (filePathCallback != null) {
                android.net.Uri[] results = null;
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new android.net.Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        results = new android.net.Uri[]{data.getData()};
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
