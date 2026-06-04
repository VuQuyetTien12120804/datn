package com.example.frontend_bookingcare.ui.account;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.account.AccountFlowListener;
import com.example.frontend_bookingcare.locale.LocaleStore;
import com.example.frontend_bookingcare.data.AuthRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.legal.LegalDocumentActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class AccountHubFragment extends Fragment implements AccountHubAdapter.Listener {

    public static final String ROW_PROFILE = "profile_detail";
    public static final String ROW_CHANGE_PWD = "change_password";
    public static final String ROW_LOGOUT = "logout";
    public static final String ROW_TERMS = "terms";
    public static final String ROW_PRIVACY = "privacy";
    public static final String ROW_SERVICE = "service_terms";
    public static final String ROW_SUPPORT = "support_line";
    public static final String ROW_RATE = "rate";
    public static final String ROW_SHARE = "share";
    public static final String ROW_FAQ = "faq";
    public static final String ROW_LANG = "language";

    private AccountHubAdapter adapter;
    private View header;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        header = view.findViewById(R.id.account_hub_header);
        RecyclerView rv = view.findViewById(R.id.account_hub_recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AccountHubAdapter(this);
        rv.setAdapter(adapter);

        refreshAll(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) refreshAll(v);
    }

    private void refreshAll(View root) {
        bindHeader();
        adapter.setItems(buildMenuRows());
    }

    private void bindHeader() {
        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null || header == null) return;
        SessionManager sm = parent.getSessionManager();
        AuthSession s = sm.getSession();
        boolean loggedIn = sm.isLoggedIn();

        TextView title = header.findViewById(R.id.account_header_title);
        TextView sub = header.findViewById(R.id.account_header_subtitle);
        LinearLayout guestActions = header.findViewById(R.id.account_header_guest_actions);
        TextView login = header.findViewById(R.id.account_header_login);
        TextView register = header.findViewById(R.id.account_header_register);
        ImageButton back = header.findViewById(R.id.account_header_back);

        AccountFlowListener flow = parent;
        login.setOnClickListener(v -> flow.openLogin());
        register.setOnClickListener(v -> flow.openRegister());

        if (loggedIn && s != null) {
            String name = s.fullName != null && !s.fullName.isEmpty() ? s.fullName : getString(R.string.account_user_default);
            title.setText(name);
            guestActions.setVisibility(View.GONE);
            sub.setVisibility(View.VISIBLE);
            sub.setText(s.email != null ? s.email : "");
        } else {
            title.setText(R.string.account_guest_label);
            guestActions.setVisibility(View.VISIBLE);
            sub.setVisibility(View.GONE);
        }

        FragmentManager cf = parent.getChildFragmentManager();
        if (cf.getBackStackEntryCount() > 0) {
            back.setVisibility(View.VISIBLE);
            back.setOnClickListener(v -> {
                try {
                    cf.executePendingTransactions();
                } catch (IllegalStateException ignored) {
                }
                if (cf.getBackStackEntryCount() > 0) {
                    cf.popBackStackImmediate();
                }
            });
        } else {
            back.setVisibility(View.INVISIBLE);
            back.setOnClickListener(null);
        }
    }

    private List<AccountListItem> buildMenuRows() {
        AccountFragment parent = (AccountFragment) getParentFragment();
        List<AccountListItem> list = new ArrayList<>();
        if (parent == null) return list;
        boolean loggedIn = parent.getSessionManager().isLoggedIn();

        if (loggedIn) {
            list.add(AccountListItem.section(getString(R.string.account_section_account)));
            list.add(AccountListItem.row(ROW_PROFILE, getString(R.string.account_menu_profile_detail),
                    getString(R.string.account_menu_profile_detail_sub), null, "👤", R.color.icon_bg_blue));
            list.add(AccountListItem.row(ROW_CHANGE_PWD, getString(R.string.change_password), null, null, "🔑", R.color.icon_bg_purple));
            list.add(AccountListItem.row(ROW_LOGOUT, getString(R.string.logout), getString(R.string.account_menu_logout_sub), null, "🚪", R.color.icon_bg_red));
        }

        list.add(AccountListItem.section(getString(R.string.account_section_terms)));
        list.add(AccountListItem.rowWithVectorIcon(ROW_TERMS, getString(R.string.account_menu_terms_use), null, null,
                R.drawable.ic_account_policy_shield, R.color.icon_bg_terms_mint));
        list.add(AccountListItem.rowWithVectorIcon(ROW_PRIVACY, getString(R.string.account_menu_privacy), null, null,
                R.drawable.ic_account_policy_lock, R.color.icon_bg_terms_lilac));
        list.add(AccountListItem.rowWithVectorIcon(ROW_SERVICE, getString(R.string.account_menu_service_terms), null, null,
                R.drawable.ic_account_policy_hand, R.color.icon_bg_terms_peach));

        list.add(AccountListItem.section(getString(R.string.account_section_support)));
        list.add(AccountListItem.row(ROW_SUPPORT, getString(R.string.account_menu_support_title),
                getString(R.string.account_menu_support_sub), null, "📞", R.color.icon_bg_cyan));
        list.add(AccountListItem.row(ROW_RATE, getString(R.string.account_menu_rate), null, null, "👍", R.color.icon_bg_yellow));
        list.add(AccountListItem.row(ROW_SHARE, getString(R.string.account_menu_share), null, null, "🔗", R.color.icon_bg_magenta));
        list.add(AccountListItem.row(ROW_FAQ, getString(R.string.account_menu_faq), null, null, "❔", R.color.icon_bg_grey));
        String langSub = LocaleStore.isEnglish(requireContext())
                ? getString(R.string.language_name_en)
                : getString(R.string.language_name_vi);
        list.add(AccountListItem.row(ROW_LANG, getString(R.string.account_menu_language), null,
                langSub, "A", R.color.icon_bg_teal));

        list.add(AccountListItem.footer());
        return list;
    }

    @Override
    public void onRowClick(@NonNull String rowId) {
        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        AccountFlowListener flow = parent;
        SessionManager sm = parent.getSessionManager();
        AuthRepository repo = parent.getAuthRepository();

        switch (rowId) {
            case ROW_PROFILE:
                flow.openProfileDetail();
                break;
            case ROW_CHANGE_PWD:
                flow.openChangePassword();
                break;
            case ROW_LOGOUT:
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.logout)
                        .setMessage(R.string.account_logout_confirm)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.logout, (d, w) ->
                                repo.logout((a, err) -> {
                                    Toast.makeText(requireContext(), R.string.account_logged_out_toast, Toast.LENGTH_SHORT).show();
                                    if (requireActivity() instanceof MainActivity) {
                                        ((MainActivity) requireActivity()).refreshAfterAuthChange();
                                    }
                                    View root = getView();
                                    if (root != null) {
                                        refreshAll(root);
                                    }
                                }))
                        .show();
                break;
            case ROW_TERMS:
                LegalDocumentActivity.start(this, LegalDocumentActivity.CODE_TERMS);
                break;
            case ROW_PRIVACY:
                LegalDocumentActivity.start(this, LegalDocumentActivity.CODE_PRIVACY);
                break;
            case ROW_SERVICE:
                LegalDocumentActivity.start(this, LegalDocumentActivity.CODE_SERVICE);
                break;
            case ROW_SUPPORT:
                dialSupport();
                break;
            case ROW_RATE:
                openStoreListing();
                break;
            case ROW_SHARE:
                shareApp();
                break;
            case ROW_FAQ:
                LegalDocumentActivity.start(this, LegalDocumentActivity.CODE_FAQ);
                break;
            case ROW_LANG:
                showLanguagePicker();
                break;
            default:
                break;
        }
    }

    private void showLanguagePicker() {
        String[] options = new String[]{
                getString(R.string.language_name_vi),
                getString(R.string.language_name_en)
        };
        int checked = LocaleStore.isEnglish(requireContext()) ? 1 : 0;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_language_dialog_title)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    String tag = which == 1 ? "en" : "vi";
                    LocaleStore.saveAndApply(requireContext(), tag);
                    dialog.dismiss();
                    Toast.makeText(requireContext(), R.string.language_changed, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void dialSupport() {
        String tel = getString(R.string.account_support_phone_uri);
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(tel)));
    }

    private void shareApp() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        send.putExtra(Intent.EXTRA_TEXT, getString(R.string.account_share_text));
        startActivity(Intent.createChooser(send, getString(R.string.account_menu_share)));
    }

    private void openStoreListing() {
        String pkg = requireContext().getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
        }
    }
}
