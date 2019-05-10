package com.zego.common.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.zego.common.R;
import com.zego.common.databinding.ActivityWebviewBinding;

/**
 * WebActivity
 * <p>
 * 一个简单的浏览器，调用 {@link #actionStart(Activity, String, String)} 即可跳转到指定url页面
 */
public class WebActivity extends BaseActivity {

    private ActivityWebviewBinding binding;
    private String url;

    public static void actionStart(Activity activity, String url, String title) {
        Intent intent = new Intent(activity, WebActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("title", title);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_webview);
        binding.title.setTitleName("");

        binding.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress >= 100) {
                    binding.pbProgress.setVisibility(View.GONE);
                } else {
                    binding.pbProgress.setVisibility(View.VISIBLE);
                    binding.pbProgress.setProgress(newProgress);
                }
                boolean isGoBack = binding.webView.canGoBack();
                boolean isForward = binding.webView.canGoForward();
                enableView(binding.goBack, isGoBack);
                enableView(binding.goForward, isForward);
                super.onProgressChanged(view, newProgress);
            }
        });

        WebSettings webSetting = binding.webView.getSettings();
        webSetting.setAllowFileAccess(true);
        webSetting.setJavaScriptEnabled(true);
        webSetting.setDomStorageEnabled(true);
        webSetting.setDatabaseEnabled(true);
        webSetting.setAppCacheEnabled(true);
        webSetting.setBuiltInZoomControls(true);
        binding.webView.setWebViewClient(new WebViewClient());

        url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        binding.webView.loadUrl(url);
        binding.title.setTitleName(title);
        binding.goBack.setEnabled(false);
    }

    public void refresh(View view) {
        binding.webView.reload();
        Toast.makeText(this, R.string.tx_web_refresh_hint, Toast.LENGTH_SHORT).show();
    }


    public void goBack(View view) {
        boolean isGoBack = binding.webView.canGoBack();
        if (isGoBack) {
            binding.webView.goBack();
        }

    }

    public void goForward(View view) {
        boolean isGoBack = binding.webView.canGoBack();
        boolean isForward = binding.webView.canGoForward();
        if (isForward) {
            binding.webView.goForward();
        }
        binding.goBack.setEnabled(isGoBack);
        binding.goForward.setEnabled(isForward);

    }


    private void enableView(View view, boolean enable) {
        if (enable) {
            view.setAlpha(1f);
        } else {
            view.setAlpha(0.3f);
        }
        view.setEnabled(enable);
    }

    public void goMenu(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.menu);
        //    指定下拉列表的显示数据
        final String[] cities = {getString(R.string.tx_menu_copy_link)};
        //    设置一个下拉的列表选择项
        builder.setItems(cities, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (cities[which].equals(getString(R.string.tx_menu_copy_link))) {
                    ClipboardManager clip = (ClipboardManager) WebActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("text", url);
                    clip.setPrimaryClip(clipData); // 复制
                    Toast.makeText(WebActivity.this, R.string.tx_copy_success, Toast.LENGTH_SHORT).show();
                }

            }
        });
        builder.show();

    }
}
