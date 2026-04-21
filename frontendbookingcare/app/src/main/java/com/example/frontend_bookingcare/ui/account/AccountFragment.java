package com.example.frontend_bookingcare.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.account.AccountFlowListener;
import com.example.frontend_bookingcare.account.ForgotPasswordDraft;
import com.example.frontend_bookingcare.account.RegisterDraft;
import com.example.frontend_bookingcare.data.AuthRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.RoleRouter;
import com.example.frontend_bookingcare.session.SessionManager;

public class AccountFragment extends Fragment implements AccountFlowListener, VerifyOtpFragment.RegisterPasswordOpening {

    public final RegisterDraft registerDraft = new RegisterDraft();
    public final ForgotPasswordDraft forgotPasswordDraft = new ForgotPasswordDraft();

    private SessionManager sessionManager;
    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        authRepository = new AuthRepository(sessionManager);
        if (savedInstanceState == null) {
            showInitialChild();
        }
    }

    private void showInitialChild() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, new AccountHubFragment())
                .commit();
    }

    private void goHomeOnMainActivity() {
        AuthSession s = sessionManager != null ? sessionManager.getSession() : null;
        Intent roleIntent = RoleRouter.intentFor(requireContext(), s);
        if (roleIntent != null) {
            // Khi vừa đăng nhập thành công ở tab Account, chuyển hẳn sang Activity
            // dành cho role đó và xoá task cũ để không còn đường back về Home bệnh nhân.
            roleIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(roleIntent);
            requireActivity().finishAffinity();
            return;
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToHomeAfterAuth();
        }
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public AuthRepository getAuthRepository() {
        return authRepository;
    }

    /**
     * Quay lại một bước trong luồng con (đăng nhập / đăng ký). Dùng {@code popBackStackImmediate}
     * để nút back phản hồi ngay, tránh {@code popBackStack()} bất đồng bộ không thấy tác dụng.
     */
    public boolean popChildFragmentBack() {
        Fragment inner = getChildFragmentManager().findFragmentById(R.id.account_inner_container);
        if (inner != null) {
            return AccountUiHelper.popAccountInnerBack(inner);
        }
        FragmentManager fm = getChildFragmentManager();
        try {
            fm.executePendingTransactions();
        } catch (IllegalStateException ignored) {
        }
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();
            return true;
        }
        return false;
    }

    /** Gỡ hết back stack con và để lại màn hub (không thêm entry mới). */
    public void clearChildBackStackToHub() {
        FragmentManager fm = getChildFragmentManager();
        try {
            fm.executePendingTransactions();
        } catch (IllegalStateException ignored) {
        }
        while (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();
        }
    }

    /** Sau khi biết email đã có tài khoản: về hub, mở đăng nhập với email sẵn điền. */
    public void navigateToLoginPrefilled(String email) {
        clearChildBackStackToHub();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, LoginFragment.newInstance(email))
                .addToBackStack(null)
                .commit();
    }

    /** Về hub và mở lại bước nhập email đăng ký (OTP trước đó không còn dùng cho tài khoản mới). */
    public void navigateToRegisterFresh() {
        registerDraft.clear();
        clearChildBackStackToHub();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, new RegisterEmailFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openLogin() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, LoginFragment.newInstance(null))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openForgotPassword() {
        forgotPasswordDraft.otp = "";
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, new ForgotPasswordEmailFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openForgotPasswordOtp() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, new ForgotPasswordOtpFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openForgotPasswordNewPassword() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, new ForgotPasswordNewPasswordFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openRegister() {
        registerDraft.clear();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, new RegisterEmailFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openVerifyOtp() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, new VerifyOtpFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openRegisterPassword() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, new RegisterPasswordFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openChangePassword() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, new ChangePasswordFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openEditProfile() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, new EditProfileFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openProfileDetail() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.account_inner_container, new ProfileFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onLoggedIn() {
        FragmentManager fm = getChildFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction()
                .replace(R.id.account_inner_container, new AccountHubFragment())
                .commit();
        goHomeOnMainActivity();
    }

    @Override
    public void onRegisterComplete() {
        registerDraft.clear();
        FragmentManager fm = getChildFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction()
                .replace(R.id.account_inner_container, new AccountHubFragment())
                .commit();
        goHomeOnMainActivity();
    }
}
