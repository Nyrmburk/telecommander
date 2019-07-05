package com.cdombroski.telecommander;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebInterfaceActivity extends AppCompatActivity {

    WebView webInterface = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_interface);
        webInterface = findViewById(R.id.WebInterface);
        webInterface.setBackgroundColor(Color.TRANSPARENT);
        webInterface.loadUrl("https://cdombroski.com");
    }
}
