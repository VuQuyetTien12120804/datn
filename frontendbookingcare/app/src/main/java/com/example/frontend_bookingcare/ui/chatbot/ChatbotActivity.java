package com.example.frontend_bookingcare.ui.chatbot;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.ChatbotMessageDto;
import com.example.frontend_bookingcare.data.ChatbotRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.support.chat.ChatAdapter;
import com.example.frontend_bookingcare.ui.support.chat.ChatMessage;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Màn hình chatbot — gọi API rule-based ở backend (/api/v1/chatbot/**).
 *
 * <p>Luồng:
 * <ol>
 *   <li>onCreate → mở session (POST /sessions) → load lịch sử (GET messages).</li>
 *   <li>User gõ + bấm Send → POST messages → backend trả 2 message
 *       (user echo + assistant reply) → append vào RecyclerView.</li>
 *   <li>Reply có suggestions → render thành chip phía trên input bar; bấm chip =
 *       gửi nội dung chip như message thường.</li>
 * </ol>
 */
public class ChatbotActivity extends AppCompatActivity {

    public static Intent newIntent(Context ctx) {
        return new Intent(ctx, ChatbotActivity.class);
    }

    private final ChatAdapter adapter = new ChatAdapter();
    private final ChatbotRepository repo = new ChatbotRepository();
    private boolean thinkingShown;

    private RecyclerView rv;
    private EditText input;
    private ImageButton send;
    private ChipGroup suggestions;
    private View bottomInset;

    private String bearer;
    private long sessionId = -1L;
    private boolean sending;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_chatbot);

        SessionManager sm = new SessionManager(this);
        AuthSession s = sm.getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            Toast.makeText(this, R.string.chatbot_login_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        bearer = "Bearer " + s.accessToken;

        View header = findViewById(R.id.cb_header_bar);
        bottomInset = findViewById(R.id.cb_bottom_inset);
        applyTopInsetToHeader(header);
        applySystemBarBottomInset(bottomInset);

        ImageButton back = findViewById(R.id.cb_back);
        back.setOnClickListener(v -> finish());

        rv = findViewById(R.id.cb_list);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        suggestions = findViewById(R.id.cb_suggestions);
        input = findViewById(R.id.cb_input);
        send = findViewById(R.id.cb_send);

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                doSend();
                return true;
            }
            return false;
        });
        send.setOnClickListener(v -> doSend());

        openSessionAndLoad();
    }

    private void openSessionAndLoad() {
        repo.openSession(bearer, (id, err) -> {
            if (err != null || id == null) {
                Toast.makeText(this, err != null ? err : "Không mở được phiên",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            sessionId = id;
            repo.listMessages(bearer, sessionId, (list, err2) -> {
                if (err2 != null) {
                    Toast.makeText(this, err2, Toast.LENGTH_SHORT).show();
                    return;
                }
                applyHistory(list);
            });
        });
    }

    private void applyHistory(@Nullable List<ChatbotMessageDto> list) {
        List<ChatMessage> built = new ArrayList<>();
        if (list != null) {
            for (ChatbotMessageDto d : list) {
                built.add(new ChatMessage("user".equalsIgnoreCase(d.sender), d.content, d.createdAtMs));
            }
        }
        adapter.setItems(built);
        scrollToBottom();
        // Sau khi load lịch sử cũ, hiển thị bộ suggestions mặc định để user dễ bắt đầu.
        renderDefaultSuggestions();
    }

    private void doSend() {
        if (sending || sessionId <= 0) return;
        String text = input.getText() != null ? input.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        sending = true;
        send.setEnabled(false);
        input.setText("");
        suggestions.removeAllViews();

        // Append ngay user message để UX nhanh.
        long now = System.currentTimeMillis();
        adapter.add(new ChatMessage(true, text, now));
        scrollToBottom();

        // Append placeholder "đang trả lời" — đánh dấu thinkingShown để xoá đúng 1 lần.
        adapter.add(new ChatMessage(false, getString(R.string.chatbot_thinking), now));
        thinkingShown = true;
        scrollToBottom();

        repo.sendMessage(bearer, sessionId, text, (data, err) -> {
            sending = false;
            send.setEnabled(true);

            // Luôn xoá placeholder nếu đang hiển thị, bất kể thành công hay lỗi.
            if (thinkingShown) {
                adapter.removeLast();
                thinkingShown = false;
            }

            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                return;
            }
            ChatbotMessageDto assistant = pickAssistant(data);
            if (assistant != null) {
                adapter.add(new ChatMessage(false, assistant.content, assistant.createdAtMs));
                scrollToBottom();
                renderSuggestions(assistant.suggestions);
            }
        });
    }

    @Nullable
    private static ChatbotMessageDto pickAssistant(@Nullable List<ChatbotMessageDto> list) {
        if (list == null) return null;
        for (int i = list.size() - 1; i >= 0; i--) {
            ChatbotMessageDto d = list.get(i);
            if (d != null && "assistant".equalsIgnoreCase(d.sender)) return d;
        }
        return null;
    }

    private void renderSuggestions(@Nullable List<String> chips) {
        suggestions.removeAllViews();
        if (chips == null || chips.isEmpty()) return;
        for (String label : chips) {
            if (label == null || label.isEmpty()) continue;
            Chip chip = new Chip(this);
            chip.setText(label);
            chip.setChipBackgroundColorResource(R.color.surface_muted);
            chip.setTextColor(getColor(R.color.text_primary));
            chip.setOnClickListener(v -> {
                input.setText(label);
                doSend();
            });
            suggestions.addView(chip);
        }
    }

    /** Bộ chip mặc định khi mới mở app — giúp user dễ bắt đầu. */
    private void renderDefaultSuggestions() {
        renderSuggestions(Arrays.asList(
                "Đặt lịch khám", "Hủy lịch hẹn", "Giờ làm việc", "Hotline phòng khám"
        ));
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            rv.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    // ---------- inset helpers (giống CustomerCareChatActivity) ----------

    private void applyTopInsetToHeader(View header) {
        final int basePadTop = header.getPaddingTop();
        final int basePadBottom = header.getPaddingBottom();
        final int basePadStart = header.getPaddingStart();
        final int basePadEnd = header.getPaddingEnd();
        final int extra = dp(6);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPaddingRelative(basePadStart, basePadTop + topInset + extra, basePadEnd, basePadBottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    private void applySystemBarBottomInset(View spacer) {
        ViewCompat.setOnApplyWindowInsetsListener(spacer, (v, insets) -> {
            int sysBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp.height != sysBottom) {
                lp.height = sysBottom;
                v.setLayoutParams(lp);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(spacer);
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
