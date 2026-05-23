package com.example.frontend_bookingcare.ui.legal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.LegalDocumentDto;
import com.example.frontend_bookingcare.data.LegalRepository;

import java.util.Locale;

public class LegalDocumentActivity extends AppCompatActivity {

    public static final String EXTRA_CODE = "legal_code";
    public static final String CODE_TERMS = "TERMS";
    public static final String CODE_PRIVACY = "PRIVACY";
    public static final String CODE_SERVICE = "SERVICE";
    public static final String CODE_FAQ = "FAQ";

    public static void start(@NonNull Fragment fragment, @NonNull String code) {
        Intent i = new Intent(fragment.requireContext(), LegalDocumentActivity.class);
        i.putExtra(EXTRA_CODE, code);
        fragment.startActivity(i);
    }

    private WebView webView;
    private ProgressBar progress;
    private TextView headerTitle;
    private final LegalRepository legalRepository = new LegalRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_legal_document);
        applyHeaderInsets();
        applyContentBottomInset();

        String code = getIntent() != null ? getIntent().getStringExtra(EXTRA_CODE) : null;
        if (code == null || code.isEmpty()) {
            finish();
            return;
        }
        String upper = code.trim().toUpperCase(Locale.ROOT);

        webView = findViewById(R.id.legal_webview);
        progress = findViewById(R.id.legal_progress);
        headerTitle = findViewById(R.id.legal_header_title);
        headerTitle.setText(placeholderTitle(upper));

        ImageButton back = findViewById(R.id.legal_back);
        back.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull WebResourceRequest request) {
                Uri u = request.getUrl();
                if (u != null && "mailto".equals(u.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_SENDTO, u));
                    return true;
                }
                return false;
            }
        });

        legalRepository.fetchByCode(upper, (dto, err) -> {
            if (isFinishing() || isDestroyed()) return;
            if (err != null || dto == null || dto.bodyHtml == null) {
                String msg = err != null ? err : getString(R.string.legal_error_loading);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                progress.setVisibility(android.view.View.GONE);
                return;
            }
            if (dto.title != null && !dto.title.isEmpty()) {
                headerTitle.setText(dto.title);
            }
            String html = wrapAsHtml(dto.bodyHtml);
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            progress.setVisibility(android.view.View.GONE);
        });
    }

    @Override
    public void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    private String placeholderTitle(String code) {
        return switch (code) {
            case CODE_TERMS -> getString(R.string.account_menu_terms_use);
            case CODE_PRIVACY -> getString(R.string.account_menu_privacy);
            case CODE_SERVICE -> getString(R.string.account_menu_service_terms);
            case CODE_FAQ -> getString(R.string.account_menu_faq);
            default -> getString(R.string.legal_default_title);
        };
    }

    private String wrapAsHtml(String body) {
        return "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>"
                + "<style>body{margin:0;padding:16px 18px 24px 18px;font-family:sans-serif;color:#111827;font-size:15px;"
                + "line-height:1.55;} h1{font-size:20px;font-weight:700;margin:0 0 12px 0;}"
                + "h2{font-size:16px;font-weight:700;margin:18px 0 8px 0;} p{margin:0 0 10px 0;}"
                + "a{color:#0B84FF;}</style></head><body>" + body + "</body></html>";
    }

    /**
     * Đẩy nội dung header (nút back, tiêu đề) xuống dưới status bar và vùng camera (display cutout).
     */
    private void applyHeaderInsets() {
        android.view.View headerBar = findViewById(R.id.legal_header_bar);
        int extraTopPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8f, getResources().getDisplayMetrics());
        ViewCompat.setOnApplyWindowInsetsListener(headerBar, (v, windowInsets) -> {
            Insets inset = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPaddingRelative(
                    v.getPaddingStart(),
                    inset.top + extraTopPx,
                    v.getPaddingEnd(),
                    v.getPaddingBottom());
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(headerBar);
    }

    private void applyContentBottomInset() {
        android.view.View host = findViewById(R.id.legal_content_host);
        ViewCompat.setOnApplyWindowInsetsListener(host, (v, windowInsets) -> {
            int bottom = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPaddingRelative(0, 0, 0, bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(host);
    }
}
