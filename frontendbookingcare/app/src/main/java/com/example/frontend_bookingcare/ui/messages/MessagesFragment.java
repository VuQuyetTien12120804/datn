package com.example.frontend_bookingcare.ui.messages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.net.Uri;
import android.util.TypedValue;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.chat.ChatStore;
import com.example.frontend_bookingcare.ui.chat.ChatThreadMeta;
import com.example.frontend_bookingcare.ui.common.HeaderInsets;
import com.example.frontend_bookingcare.ui.support.chat.ChatMessage;
import com.example.frontend_bookingcare.ui.support.chat.CustomerCareChatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

public class MessagesFragment extends Fragment {

    private final com.example.frontend_bookingcare.data.PatientAppointmentsRepository apptRepo =
            new com.example.frontend_bookingcare.data.PatientAppointmentsRepository();
    private ThreadsAdapter adapter;
    private ChatStore store;
    private List<MessageThread> all = new ArrayList<>();
    private String query = "";

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

        RecyclerView rv = view.findViewById(R.id.messages_list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        String patientName = (s != null && s.fullName != null && !s.fullName.isEmpty()) ? s.fullName : "Tien";

        store = new ChatStore(requireContext());

        // Seed demo doctor threads only once (if empty)
        long now = System.currentTimeMillis();
        store.seedIfEmpty(
                "doctor:1",
                new ChatThreadMeta("doctor:1", "Bác sĩ Nguyễn Thị Bích Đào", patientName, true, 0, ""),
                java.util.List.of(new ChatMessage(false, "Bạn có lịch hẹn khám với Nguyễn...", now - 5L * 24 * 3600_000))
        );
        store.seedIfEmpty(
                "doctor:2",
                new ChatThreadMeta("doctor:2", "Bác sĩ Tô Lang Châu", patientName, false, 0, ""),
                java.util.List.of(new ChatMessage(false, "Chào A T nha, a cần đk khám ch...", now - 6L * 24 * 3600_000))
        );
        store.seedIfEmpty(
                "doctor:3",
                new ChatThreadMeta("doctor:3", "Bác sĩ Hà Thị Ngọc Bích", patientName, false, 0, ""),
                java.util.List.of(new ChatMessage(false, "Yêu cầu tư vấn của bạn đã được...", now - 8L * 24 * 3600_000))
        );
        store.seedIfEmpty(
                "support:cskh",
                new ChatThreadMeta("support:cskh", "Chăm Sóc Khách Hàng", patientName, false, 0, ""),
                java.util.List.of(new ChatMessage(false, "Xin chào bạn, mình có thể hỗ trợ gì ạ?", now - 60_000))
        );

        adapter = new ThreadsAdapter(t ->
                startActivity(CustomerCareChatActivity.newIntent(
                        requireContext(),
                        t.threadId,
                        t.title,
                        t.patientName,
                        t.locked
                )));
        rv.setAdapter(adapter);

        all = toUiThreads(store.listThreadsSorted());
        adapter.setItems(filter(all, query));

        EditText search = view.findViewById(R.id.messages_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s != null ? s.toString().trim() : "";
                if (adapter != null) adapter.setItems(filter(all, query));
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Build real threads from appointments (unique doctorId -> doctorName)
        if (sm.isLoggedIn() && s != null && s.accessToken != null && !s.accessToken.isEmpty()) {
            String bearer = "Bearer " + s.accessToken;
            loadFromAppointments(bearer, patientName);
        }

        FloatingActionButton fab = view.findViewById(R.id.messages_fab);
        fab.setOnClickListener(v -> {
            Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:19002805"));
            startActivity(dial);
        });
    }

    // toolbar insets handled by HeaderInsets

    private static List<MessageThread> toUiThreads(List<ChatThreadMeta> metas) {
        List<MessageThread> out = new ArrayList<>();
        if (metas == null) return out;
        long now = System.currentTimeMillis();
        for (ChatThreadMeta m : metas) {
            if (m == null) continue;
            String time = TimeAgo.format(now, m.updatedAtMs);
            out.add(new MessageThread(
                    m.threadId != null ? m.threadId : "",
                    m.title != null ? m.title : "Tin nhắn",
                    m.subtitle != null ? m.subtitle : "",
                    m.lastMessage != null ? m.lastMessage : "",
                    time,
                    m.locked
                    , m.unreadCount
            ));
        }
        return out;
    }

    private void loadFromAppointments(String bearer, String patientName) {
        // fetch 3 groups, merge unique doctor threads
        HashSet<Integer> doctorIds = new HashSet<>();
        java.util.function.BiConsumer<java.util.List<com.example.frontend_bookingcare.api.PatientAppointmentDto>, String> ingest = (list, group) -> {
            if (list == null) return;
            for (com.example.frontend_bookingcare.api.PatientAppointmentDto a : list) {
                if (a == null || a.doctorId == null) continue;
                if (doctorIds.contains(a.doctorId)) continue;
                doctorIds.add(a.doctorId);
                String docName = a.doctorName != null ? a.doctorName : ("Bác sĩ #" + a.doctorId);
                boolean locked = a.status != null && ("pending".equalsIgnoreCase(a.status) || "confirmed".equalsIgnoreCase(a.status) || "checked_in".equalsIgnoreCase(a.status));
                store.upsertThread(new ChatThreadMeta("doctor:" + a.doctorId, docName, patientName, locked, 0, ""));
            }
        };

        apptRepo.fetch(bearer, "UPCOMING", (data, err) -> {
            ingest.accept(data, "UPCOMING");
            apptRepo.fetch(bearer, "COMPLETED", (d2, e2) -> {
                ingest.accept(d2, "COMPLETED");
                apptRepo.fetch(bearer, "CANCELLED", (d3, e3) -> {
                    ingest.accept(d3, "CANCELLED");
                    // refresh UI from store after all
                    all = toUiThreads(store.listThreadsSorted());
                    if (adapter != null) adapter.setItems(filter(all, query));
                });
            });
        });
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
