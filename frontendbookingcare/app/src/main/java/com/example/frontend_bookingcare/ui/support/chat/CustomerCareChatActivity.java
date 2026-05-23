package com.example.frontend_bookingcare.ui.support.chat;



import android.content.Context;

import android.content.Intent;

import android.os.Bundle;

import android.text.Editable;

import android.text.TextUtils;

import android.util.TypedValue;

import android.view.View;

import android.view.ViewGroup;

import android.view.inputmethod.EditorInfo;

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

import com.example.frontend_bookingcare.api.ChatMessageDto;

import com.example.frontend_bookingcare.data.ChatRepository;

import com.example.frontend_bookingcare.session.AuthSession;

import com.example.frontend_bookingcare.session.ProfileExtras;

import com.example.frontend_bookingcare.session.SessionManager;

import com.example.frontend_bookingcare.ui.common.UnicodeInputHelper;



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

    private static final String EXTRA_AUDIENCE = "audience";



    public static Intent newIntent(Context ctx) {

        return newIntent(ctx, "support:cskh", ctx.getString(R.string.support_cskh_title), null, false);

    }



    public static Intent newIntent(Context ctx, String threadId, String title, @Nullable String subtitle, boolean locked) {

        return newIntent(ctx, threadId, title, subtitle, locked, ChatRepository.Audience.PATIENT);

    }



    public static Intent newDoctorIntent(Context ctx, String threadId, String title, @Nullable String subtitle, boolean locked) {

        return newIntent(ctx, threadId, title, subtitle, locked, ChatRepository.Audience.DOCTOR);

    }



    public static Intent newIntent(Context ctx, String threadId, String title, @Nullable String subtitle,

                                   boolean locked, ChatRepository.Audience audience) {

        Intent i = new Intent(ctx, CustomerCareChatActivity.class);

        i.putExtra(EXTRA_THREAD_ID, threadId);

        i.putExtra(EXTRA_TITLE, title);

        i.putExtra(EXTRA_SUBTITLE, subtitle);

        i.putExtra(EXTRA_LOCKED, locked);

        i.putExtra(EXTRA_AUDIENCE, audience.name());

        return i;

    }



    private final ChatAdapter adapter = new ChatAdapter();

    private final List<ChatMessage> messages = new ArrayList<>();

    private final ChatRepository chatRepo = new ChatRepository();



    private String threadIdFinal;

    private ChatRepository.Audience audience = ChatRepository.Audience.PATIENT;

    private boolean locked;

    private String bearer;

    private RecyclerView rv;

    private EditText input;

    private ImageButton send;

    private View inputBar;

    private View lockedBanner;

    private View bottomInset;

    private long lastMessageFetchMs;

    private static final long MESSAGE_RELOAD_DEBOUNCE_MS = 1500;



    @Nullable private List<ChatMessage> pendingMessages;

    private boolean pendingScrollToEnd;



    @Override

    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_customer_care_chat);



        String threadId = getIntent().getStringExtra(EXTRA_THREAD_ID);

        if (threadId == null || threadId.trim().isEmpty()) threadId = "support:cskh";

        threadIdFinal = threadId;

        String title = getIntent().getStringExtra(EXTRA_TITLE);

        locked = getIntent().getBooleanExtra(EXTRA_LOCKED, false);

        String audienceRaw = getIntent().getStringExtra(EXTRA_AUDIENCE);

        if ("DOCTOR".equalsIgnoreCase(audienceRaw)) {

            audience = ChatRepository.Audience.DOCTOR;

        }



        SessionManager sm = new SessionManager(this);

        AuthSession s = sm.getSession();

        if (s == null || s.accessToken == null || s.accessToken.isEmpty()) {

            Toast.makeText(this, R.string.chat_login_required, Toast.LENGTH_SHORT).show();

            finish();

            return;

        }

        bearer = "Bearer " + s.accessToken;



        View header = findViewById(R.id.cc_header_bar);

        inputBar = findViewById(R.id.cc_input_bar);

        bottomInset = findViewById(R.id.cc_bottom_inset);

        lockedBanner = findViewById(R.id.cc_locked_banner);

        ImageButton back = findViewById(R.id.cc_back);

        TextView titleView = findViewById(R.id.cc_title);

        TextView subtitleView = findViewById(R.id.cc_subtitle);



        applyTopInsetToHeader(header);

        applySystemBarBottomInset(bottomInset);

        back.setOnClickListener(v -> finish());



        String resolvedTitle = (title != null && !title.isEmpty()) ? title : getString(R.string.support_cskh_title);

        titleView.setText(resolvedTitle);



        ProfileExtras extras = sm.getProfileExtras();

        String extraSubtitle = getIntent().getStringExtra(EXTRA_SUBTITLE);

        String subtitle = !TextUtils.isEmpty(extraSubtitle)

                ? extraSubtitle

                : buildPatientSubtitle(this, s, extras);

        subtitleView.setText(subtitle);



        rv = findViewById(R.id.cc_list);

        rv.setLayoutManager(new LinearLayoutManager(this));

        rv.setAdapter(adapter);



        input = findViewById(R.id.cc_input);

        UnicodeInputHelper.enableChatInput(input);

        input.setOnEditorActionListener((v, actionId, event) -> {

            if (actionId == EditorInfo.IME_ACTION_SEND) {

                doSend();

                return true;

            }

            return false;

        });

        input.setOnFocusChangeListener((v, hasFocus) -> {

            if (!hasFocus) {

                flushPendingMessages();

            }

        });

        send = findViewById(R.id.cc_send);



        send.setOnClickListener(v -> doSend());

        applyLockedState();



        loadMessages(true);

    }



    private void applyLockedState() {

        if (!locked) return;

        if (lockedBanner != null) lockedBanner.setVisibility(View.VISIBLE);

        if (inputBar != null) inputBar.setVisibility(View.GONE);

        if (input != null) {

            input.setEnabled(false);

            input.setHint(R.string.chat_locked_hint);

        }

        if (send != null) send.setEnabled(false);

    }



    @Override

    protected void onResume() {

        super.onResume();

        if (bearer != null) {

            long now = System.currentTimeMillis();

            if (now - lastMessageFetchMs > MESSAGE_RELOAD_DEBOUNCE_MS) {

                loadMessages(false);

            }

        }

    }



    private void loadMessages(boolean scrollToEnd) {

        if (shouldDeferMessageUpdate()) {

            pendingScrollToEnd = scrollToEnd || pendingScrollToEnd;

            return;

        }

        lastMessageFetchMs = System.currentTimeMillis();

        chatRepo.markRead(audience, bearer, threadIdFinal, (ignored, err) -> {

            // non-blocking

        });

        chatRepo.fetchMessages(audience, bearer, threadIdFinal, (data, err) -> runOnUiThread(() -> {

            if (err != null) {

                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();

                return;

            }

            List<ChatMessage> parsed = parseMessages(data);

            if (shouldDeferMessageUpdate()) {

                pendingMessages = parsed;

                pendingScrollToEnd = scrollToEnd || pendingScrollToEnd;

                return;

            }

            applyMessages(parsed, scrollToEnd);

        }));

    }



    private List<ChatMessage> parseMessages(@Nullable List<ChatMessageDto> data) {

        List<ChatMessage> parsed = new ArrayList<>();

        if (data == null) return parsed;

        for (ChatMessageDto dto : data) {

            if (dto == null) continue;

            parsed.add(new ChatMessage(dto.fromMe, dto.content, dto.createdAtMs));

        }

        return parsed;

    }



    private void applyMessages(List<ChatMessage> parsed, boolean scrollToEnd) {

        messages.clear();

        messages.addAll(parsed);

        adapter.setItems(messages);

        if (scrollToEnd) {

            rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));

        }

    }



    private boolean shouldDeferMessageUpdate() {

        if (input == null || !input.hasFocus()) return false;

        Editable text = input.getText();

        return text != null && (text.length() > 0 || UnicodeInputHelper.isImeComposing(text));

    }



    private void flushPendingMessages() {

        if (pendingMessages == null) return;

        List<ChatMessage> pending = pendingMessages;

        boolean scroll = pendingScrollToEnd;

        pendingMessages = null;

        pendingScrollToEnd = false;

        applyMessages(pending, scroll);

    }



    private void doSend() {

        if (locked) {

            Toast.makeText(this, R.string.chat_locked_hint, Toast.LENGTH_SHORT).show();

            return;

        }

        String text = input.getText() != null ? input.getText().toString().trim() : "";

        if (text.isEmpty()) return;

        send.setEnabled(false);

        chatRepo.sendMessage(audience, bearer, threadIdFinal, text, (dto, err) -> runOnUiThread(() -> {

            send.setEnabled(true);

            if (err != null) {

                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();

                return;

            }

            input.setText("");

            loadMessages(true);

        }));

    }



    private static String buildPatientSubtitle(Context ctx, @Nullable AuthSession session, @Nullable ProfileExtras extras) {

        String name = session != null && !TextUtils.isEmpty(session.fullName)

                ? session.fullName : ctx.getString(R.string.dash_placeholder);

        Integer age = extras != null ? computeAge(extras.dob) : null;

        if (age != null) {

            return ctx.getString(R.string.chat_patient_subtitle_age_fmt, name, age);

        }

        return ctx.getString(R.string.appt_patient_fmt, name);

    }



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

            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;

            v.setPaddingRelative(basePadStart, basePadTop + topInset + extra, basePadEnd, basePadBottom);

            return insets;

        });

        ViewCompat.requestApplyInsets(header);

    }



    /** Nav-bar inset only — IME is handled by adjustResize; do not pad input bar with IME height. */

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

        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));

    }

}


