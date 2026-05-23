package com.bookingcare.backend_bookingcare.security;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.entity.Account;
import com.bookingcare.backend_bookingcare.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentAccountService {

    private final AccountRepository accountRepository;

    public AuthUser requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new ApiException(401, "Unauthorized");
        }
        return user;
    }

    public Account requireAccount() {
        AuthUser user = requireUser();
        return accountRepository.findById(user.getAccountId())
                .orElseThrow(() -> new ApiException(401, "Unauthorized"));
    }

    public void requireRole(String roleCode) {
        String role = requireUser().getRoleCode();
        if (!roleCode.equalsIgnoreCase(role)) {
            throw new ApiException(403, "Forbidden");
        }
    }
}
