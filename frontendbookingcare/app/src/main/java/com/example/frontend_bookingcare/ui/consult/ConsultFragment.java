package com.example.frontend_bookingcare.ui.consult;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.TypedValue;

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
import com.example.frontend_bookingcare.ui.messages.MessageThread;
import com.example.frontend_bookingcare.ui.messages.ThreadsAdapter;
import com.example.frontend_bookingcare.ui.messages.TimeAgo;
import com.example.frontend_bookingcare.ui.support.SupportBottomSheetDialogFragment;
import com.example.frontend_bookingcare.ui.support.chat.CustomerCareChatActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ConsultFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_consult, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar tb = view.findViewById(R.id.consult_toolbar);
        HeaderInsets.applyToToolbar(tb);

        view.findViewById(R.id.consult_btn_support).setOnClickListener(v ->
                SupportBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "support_sheet_consult"));

        RecyclerView rv = view.findViewById(R.id.consult_list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        String patientName = (s != null && s.fullName != null && !s.fullName.isEmpty()) ? s.fullName : "Tien";

        ChatStore store = new ChatStore(requireContext());
        ThreadsAdapter adapter = new ThreadsAdapter(t ->
                startActivity(CustomerCareChatActivity.newIntent(requireContext(), t.threadId, t.title, t.patientName, t.locked)));
        rv.setAdapter(adapter);

        adapter.setItems(toUiThreads(store.listThreadsSorted(), patientName));
    }

    private List<MessageThread> toUiThreads(List<ChatThreadMeta> metas, String patientName) {
        List<MessageThread> out = new ArrayList<>();
        if (metas == null) return out;
        long now = System.currentTimeMillis();
        for (ChatThreadMeta m : metas) {
            if (m == null) continue;
            String time = TimeAgo.format(now, m.updatedAtMs);
            out.add(new MessageThread(
                    m.threadId != null ? m.threadId : "",
                    m.title != null ? m.title : "Tin nhắn",
                    patientName,
                    m.lastMessage != null ? m.lastMessage : "",
                    time,
                    m.locked,
                    m.unreadCount
            ));
        }
        return out;
    }

    // toolbar insets handled by HeaderInsets
}
