package com.example.frontend_bookingcare.ui.support.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.ProfileExtras;
import com.example.frontend_bookingcare.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerCareChatActivity extends AppCompatActivity {

    private static final String EXTRA_THREAD_ID = "thread_id";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_SUBTITLE = "subtitle";
    private static final String EXTRA_LOCKED = "locked";

    public static Intent newIntent(Context ctx) {
        return newIntent(ctx, "support:cskh", "Chăm Sóc Khách Hàng", null, false);
    }

    public static Intent newIntent(Context ctx, String threadId, String title, @Nullable String subtitle, boolean locked) {
        Intent i = new Intent(ctx, CustomerCareChatActivity.class);
        i.putExtra(EXTRA_THREAD_ID, threadId);
        i.putExtra(EXTRA_TITLE, title);
        i.putExtra(EXTRA_SUBTITLE, subtitle);
        i.putExtra(EXTRA_LOCKED, locked);
        return i;
    }

    private final ChatAdapter adapter = new ChatAdapter();
    private final List<ChatMessage> messages = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_customer_care_chat);

        String threadId = getIntent().getStringExtra(EXTRA_THREAD_ID);
        if (threadId == null || threadId.trim().isEmpty()) threadId = "support:cskh";
        final String threadIdFinal = threadId;
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        boolean locked = getIntent().getBooleanExtra(EXTRA_LOCKED, false);

        View header = findViewById(R.id.cc_header_bar);
        View inputBar = findViewById(R.id.cc_input_bar);
        ImageButton back = findViewById(R.id.cc_back);
        TextView titleView = findViewById(R.id.cc_title);
        TextView subtitleView = findViewById(R.id.cc_subtitle);

        applyTopInsetToHeader(header);
        applyBottomInsetToInputBar(inputBar);
        back.setOnClickListener(v -> finish());

        String resolvedTitle = (title != null && !title.isEmpty()) ? title : "Chăm Sóc Khách Hàng";
        titleView.setText(resolvedTitle);

        SessionManager sm = new SessionManager(this);
        AuthSession s = sm.getSession();
        ProfileExtras extras = sm.getProfileExtras();
        String extraSubtitle = getIntent().getStringExtra(EXTRA_SUBTITLE);
        String subtitle = !TextUtils.isEmpty(extraSubtitle)
                ? extraSubtitle
                : buildPatientSubtitle(s, extras);
        subtitleView.setText(subtitle);

        RecyclerView rv = findViewById(R.id.cc_list);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        EditText input = findViewById(R.id.cc_input);
        ImageButton send = findViewById(R.id.cc_send);

        // Load from local store, seed if empty
        com.example.frontend_bookingcare.ui.chat.ChatStore store = new com.example.frontend_bookingcare.ui.chat.ChatStore(this);
        store.markRead(threadIdFinal);
        boolean isDoctorThread = threadIdFinal.startsWith("doctor:");
        store.seedIfEmpty(
                threadIdFinal,
                new com.example.frontend_bookingcare.ui.chat.ChatThreadMeta(threadIdFinal, resolvedTitle, subtitle, locked, 0, ""),
                java.util.List.of(new ChatMessage(false, locked
                        ? "Bạn có lịch hẹn. Hãy để lại câu hỏi, CSKH/bác sĩ sẽ phản hồi sớm."
                        : (isDoctorThread ? "Chào bạn, bạn cần bác sĩ hỗ trợ gì ạ?" : "Xin chào bạn, mình có thể hỗ trợ gì ạ?"),
                        System.currentTimeMillis() - 60_000))
        );
        messages.clear();
        messages.addAll(store.getMessages(threadIdFinal));
        adapter.setItems(messages);
        rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));

        View attach = findViewById(R.id.cc_attach);
        View photo = findViewById(R.id.cc_photo);
        attach.setOnClickListener(v -> Toast.makeText(this, "Tính năng đính kèm sẽ bổ sung sau.", Toast.LENGTH_SHORT).show());
        photo.setOnClickListener(v -> Toast.makeText(this, "Tính năng gửi ảnh sẽ bổ sung sau.", Toast.LENGTH_SHORT).show());

        send.setOnClickListener(v -> {
            if (locked) {
                Toast.makeText(this, "Cuộc trò chuyện này đang được bảo vệ.", Toast.LENGTH_SHORT).show();
                return;
            }
            String text = input.getText() != null ? input.getText().toString().trim() : "";
            if (text.isEmpty()) return;
            ChatMessage m = new ChatMessage(true, text, System.currentTimeMillis());
            messages.add(m);
            adapter.add(m);
            store.append(threadIdFinal, m);
            input.setText("");
            rv.scrollToPosition(adapter.getItemCount() - 1);
            store.markRead(threadIdFinal);

            // auto-reply demo
            rv.postDelayed(() -> {
                ChatMessage r = new ChatMessage(false, "Đã nhận được. Mình sẽ hỗ trợ bạn ngay.", System.currentTimeMillis());
                messages.add(r);
                adapter.add(r);
                store.append(threadIdFinal, r);
                rv.scrollToPosition(adapter.getItemCount() - 1);
                store.markRead(threadIdFinal);
            }, 650);
        });
    }

    private static String buildPatientSubtitle(@Nullable AuthSession session, @Nullable ProfileExtras extras) {
        String name = session != null && !TextUtils.isEmpty(session.fullName) ? session.fullName : "—";
        Integer age = extras != null ? computeAge(extras.dob) : null;
        if (age != null) {
            return "Bệnh nhân: " + name + " - " + age + " tuổi";
        }
        return "Bệnh nhân: " + name;
    }

    /** dob expected as "dd/MM/yyyy" or "yyyy-MM-dd". Returns null if unparsable. */
    @Nullable
    private static Integer computeAge(@Nullable String dob) {
        if (dob == null || dob.trim().isEmpty()) return null;
        String[] patterns = new String[]{"dd/MM/yyyy", "yyyy-MM-dd"};
        Date birth = null;
        for (String p : patterns) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat(p, Locale.getDefault());
                fmt.setLenient(false);
                birth = fmt.parse(dob.trim());
                if (birth != null) break;
            } catch (Exception ignored) {
            }
        }
        if (birth == null) return null;
        Calendar cBirth = Calendar.getInstance();
        cBirth.setTime(birth);
        Calendar now = Calendar.getInstance();
        int age = now.get(Calendar.YEAR) - cBirth.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < cBirth.get(Calendar.DAY_OF_YEAR)) age--;
        return age >= 0 ? age : null;
    }

    private void applyTopInsetToHeader(View header) {
        final int basePadTop = header.getPaddingTop();
        final int basePadBottom = header.getPaddingBottom();
        final int basePadStart = header.getPaddingStart();
        final int basePadEnd = header.getPaddingEnd();
        final int extra = dp(6);

        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPaddingRelative(basePadStart, basePadTop + topInset + extra, basePadEnd, basePadBottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    private void applyBottomInsetToInputBar(View inputBar) {
        final int basePadTop = inputBar.getPaddingTop();
        final int basePadBottom = inputBar.getPaddingBottom();
        final int basePadStart = inputBar.getPaddingStart();
        final int basePadEnd = inputBar.getPaddingEnd();

        ViewCompat.setOnApplyWindowInsetsListener(inputBar, (v, insets) -> {
            int sysBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int bottomInset = Math.max(sysBottom, imeBottom);
            v.setPaddingRelative(basePadStart, basePadTop, basePadEnd, basePadBottom + bottomInset);
            return insets;
        });
        ViewCompat.requestApplyInsets(inputBar);
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
