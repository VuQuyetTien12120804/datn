package com.example.frontend_bookingcare.ui.doctor_panel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.ChatThreadDto;
import com.example.frontend_bookingcare.data.ChatRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.messages.MessageThread;
import com.example.frontend_bookingcare.ui.messages.ThreadsAdapter;
import com.example.frontend_bookingcare.ui.messages.TimeAgo;
import com.example.frontend_bookingcare.ui.support.chat.CustomerCareChatActivity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DoctorMessagesFragment extends Fragment {

    private final ChatRepository chatRepo = new ChatRepository();
    private ThreadsAdapter adapter;
    private String bearer;
    private View emptyView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar loading;
    private View errorPanel;
    private TextView errorText;
    private MaterialButton retryBtn;
    private boolean loadedOnce;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DoctorUiHelper.applyHeroTopInset(view.findViewById(R.id.doctor_messages_hero), 16);
        DoctorEmptyUi.bindHero(view,
                R.drawable.ic_nav_doctor_messages,
                R.string.doctor_nav_messages,
                R.string.doctor_messages_subtitle);

        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        if (!sm.isLoggedIn() || s == null || s.accessToken == null || s.accessToken.isEmpty()) {
            return;
        }
        bearer = "Bearer " + s.accessToken;

        emptyView = view.findViewById(R.id.doctor_messages_empty);
        if (emptyView != null) {
            DoctorEmptyUi.bind(emptyView,
                    R.drawable.ic_nav_doctor_messages,
                    R.string.doctor_messages_empty_title,
                    R.string.doctor_messages_empty_hint);
        }

        swipeRefresh = view.findViewById(R.id.doctor_messages_refresh);
        swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.doctor_brand_primary),
                ContextCompat.getColor(requireContext(), R.color.doctor_brand_primary_light));
        swipeRefresh.setOnRefreshListener(() -> loadThreads(true, true));

        loading = view.findViewById(R.id.doctor_fetch_loading);
        errorPanel = view.findViewById(R.id.doctor_fetch_error);
        errorText = view.findViewById(R.id.doctor_fetch_error_text);
        retryBtn = view.findViewById(R.id.doctor_fetch_retry);
        retryBtn.setOnClickListener(v -> loadThreads(false, false));

        RecyclerView rv = view.findViewById(R.id.doctor_messages_list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ThreadsAdapter(t -> startActivity(CustomerCareChatActivity.newDoctorIntent(
                requireContext(),
                t.threadId,
                t.title,
                t.chatSubtitle,
                t.locked
        )), R.layout.item_doctor_message_thread);
        rv.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadThreads(false, loadedOnce);
    }

    private void loadThreads(boolean fromSwipe, boolean silent) {
        if (bearer == null || adapter == null) return;

        if (fromSwipe) {
            DoctorPanelStateUi.hide(loading, errorPanel, null);
        } else if (!silent) {
            DoctorPanelStateUi.showLoading(loading, errorPanel, swipeRefresh);
        }

        chatRepo.fetchThreads(ChatRepository.Audience.DOCTOR, bearer, (data, err) -> {
            if (!isAdded()) return;
            if (err != null) {
                DoctorPanelStateUi.showError(loading, errorPanel, errorText, retryBtn, swipeRefresh,
                        getString(R.string.doctor_panel_load_failed_fmt, err),
                        () -> loadThreads(false, false));
                return;
            }
            loadedOnce = true;
            DoctorPanelStateUi.hide(loading, errorPanel, swipeRefresh);
            List<MessageThread> threads = toUiThreads(data);
            adapter.setItems(threads);
            if (emptyView != null) {
                emptyView.setVisibility(threads.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private static List<MessageThread> toUiThreads(List<ChatThreadDto> dtos) {
        List<MessageThread> out = new ArrayList<>();
        if (dtos == null) return out;
        long now = System.currentTimeMillis();
        for (ChatThreadDto dto : dtos) {
            if (dto == null) continue;
            out.add(new MessageThread(
                    dto.threadKey != null ? dto.threadKey : "",
                    dto.title != null ? dto.title : "Bệnh nhân",
                    dto.subtitle != null ? dto.subtitle : "",
                    dto.lastMessage != null ? dto.lastMessage : "",
                    TimeAgo.format(now, dto.updatedAtMs),
                    dto.locked || !dto.canSend,
                    dto.unreadCount,
                    dto.subtitle != null ? dto.subtitle : ""
            ));
        }
        return out;
    }
}
