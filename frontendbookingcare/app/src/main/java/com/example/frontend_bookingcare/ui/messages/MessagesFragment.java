package com.example.frontend_bookingcare.ui.messages;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.ChatThreadDto;
import com.example.frontend_bookingcare.data.ChatRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.common.HeaderInsets;
import com.example.frontend_bookingcare.ui.common.PatientEmptyUi;
import com.example.frontend_bookingcare.ui.common.UnicodeInputHelper;
import com.example.frontend_bookingcare.ui.support.chat.CustomerCareChatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment {

    private final ChatRepository chatRepo = new ChatRepository();
    private ThreadsAdapter adapter;
    private List<MessageThread> all = new ArrayList<>();
    private String query = "";
    private RecyclerView rv;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar tb = view.findViewById(R.id.messages_toolbar);
        HeaderInsets.applyToToolbar(tb);

        rv = view.findViewById(R.id.messages_list);
        emptyState = view.findViewById(R.id.messages_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ThreadsAdapter(t ->
                startActivity(CustomerCareChatActivity.newIntent(
                        requireContext(),
                        t.threadId,
                        t.title,
                        t.chatSubtitle,
                        t.locked
                )));
        rv.setAdapter(adapter);

        EditText search = view.findViewById(R.id.messages_search);
        UnicodeInputHelper.enableSingleLineText(search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (UnicodeInputHelper.isImeComposing(s)) return;
                query = s != null ? s.toString().trim() : "";
                refreshVisibleThreads();
            }
        });

        // FAB tư vấn: mở thẳng chat với CSKH (cùng thread "support:cskh" với
        // SupportBottomSheetDialogFragment) — thay vì gọi điện như phiên bản trước.
        FloatingActionButton fab = view.findViewById(R.id.messages_fab);
        fab.setOnClickListener(v -> openSupportChat());
    }

    /** Mở chat CSKH; nếu chưa đăng nhập thì điều hướng sang tab Tài khoản. */
    private void openSupportChat() {
        SessionManager sm = new SessionManager(requireContext());
        if (!sm.isLoggedIn() || sm.getSession() == null
                || TextUtils.isEmpty(sm.getSession().accessToken)) {
            Toast.makeText(requireContext(), R.string.chat_login_required, Toast.LENGTH_SHORT).show();
            openAccountTab();
            return;
        }
        startActivity(CustomerCareChatActivity.newIntent(
                requireContext(),
                "support:cskh",
                getString(R.string.support_cskh_title),
                null,
                false));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadThreads();
    }

    /**
     * MainActivity đổi tab bằng hide/show — onResume không chạy lại khi quay lại tab này.
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadThreads();
        }
    }

    public void reloadForSessionChange() {
        all.clear();
        query = "";
        if (isAdded()) loadThreads();
    }

    private void loadThreads() {
        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        if (!sm.isLoggedIn() || s == null || s.accessToken == null || s.accessToken.isEmpty()) {
            all = new ArrayList<>();
            refreshVisibleThreads();
            return;
        }
        String bearer = "Bearer " + s.accessToken;
        chatRepo.fetchThreads(ChatRepository.Audience.PATIENT, bearer, (data, err) -> {
            if (!isAdded()) return;
            if (err != null) {
                all = new ArrayList<>();
                refreshVisibleThreads();
                if (emptyState != null && rv != null) {
                    emptyState.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.GONE);
                    PatientEmptyUi.bindMessage(emptyState, R.string.messages_empty_title, err);
                }
                return;
            }
            all = toUiThreads(requireContext(), data, getString(R.string.messages_title));
            refreshVisibleThreads();
        });
    }

    private void refreshVisibleThreads() {
        List<MessageThread> visible = filter(all, query);
        applyList(visible);
        updateEmptyState(visible);
    }

    private void applyList(List<MessageThread> list) {
        if (adapter != null) adapter.setItems(list);
    }

    private void updateEmptyState(@NonNull List<MessageThread> visible) {
        if (emptyState == null || rv == null) return;
        SessionManager sm = new SessionManager(requireContext());
        if (!sm.isLoggedIn()) {
            emptyState.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
            PatientEmptyUi.bind(emptyState, android.R.drawable.ic_lock_lock,
                    R.string.messages_login_title, R.string.messages_login_hint);
            emptyState.setOnClickListener(v -> openAccountTab());
            return;
        }
        if (!visible.isEmpty()) {
            emptyState.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            emptyState.setOnClickListener(null);
            return;
        }
        emptyState.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(query) && !all.isEmpty()) {
            PatientEmptyUi.bind(emptyState, android.R.drawable.ic_menu_search,
                    R.string.search_no_results_title, R.string.search_no_results_hint);
        } else {
            PatientEmptyUi.bind(emptyState, android.R.drawable.ic_dialog_email,
                    R.string.messages_empty_title, R.string.messages_empty_hint);
        }
        emptyState.setOnClickListener(null);
    }

    private void openAccountTab() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).switchToTab(R.id.nav_account);
        }
    }

    private static List<MessageThread> toUiThreads(Context ctx, List<ChatThreadDto> dtos, String defaultTitle) {
        List<MessageThread> out = new ArrayList<>();
        if (dtos == null) return out;
        long now = System.currentTimeMillis();
        for (ChatThreadDto dto : dtos) {
            if (dto == null) continue;
            String time = TimeAgo.format(ctx, now, dto.updatedAtMs);
            String rawSub = dto.subtitle != null ? dto.subtitle.trim() : "";
            String listSub = subtitleForList(rawSub);
            String chatSub = !rawSub.isEmpty() ? rawSub : listSub;
            boolean locked = dto.locked || !dto.canSend;
            out.add(new MessageThread(
                    dto.threadKey != null ? dto.threadKey : "",
                    dto.title != null ? dto.title : defaultTitle,
                    listSub,
                    dto.lastMessage != null ? dto.lastMessage : "",
                    time,
                    locked,
                    dto.unreadCount,
                    chatSub
            ));
        }
        return out;
    }

    /** Bỏ dòng phụ là tên bệnh nhân — chỉ giữ thông tin bác sĩ / CSKH. */
    private static String subtitleForList(@NonNull String raw) {
        if (raw.isEmpty()) return "";
        String lower = raw.toLowerCase();
        if (lower.startsWith("bệnh nhân:") || lower.startsWith("benh nhan:")
                || lower.startsWith("patient:")) {
            return "";
        }
        return raw;
    }

    private static List<MessageThread> filter(List<MessageThread> list, String q) {
        if (list == null) return new ArrayList<>();
        if (q == null || q.trim().isEmpty()) return list;
        String needle = q.trim().toLowerCase();
        List<MessageThread> out = new ArrayList<>();
        for (MessageThread t : list) {
            String hay = (t.title != null ? t.title : "") + " " + (t.preview != null ? t.preview : "");
            if (hay.toLowerCase().contains(needle)) out.add(t);
        }
        return out;
    }
}
