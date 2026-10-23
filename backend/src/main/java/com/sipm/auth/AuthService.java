package com.sipm.auth;

import com.sipm.auth.dto.LoginRequest;
import com.sipm.auth.dto.LoginResponse;
import com.sipm.security.AppUserPrincipal;
import com.sipm.security.JwtTokenProvider;
import com.sipm.user.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
                       UserService userService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    /** Throws BadCredentialsException / DisabledException, mapped to 401 by the exception handler. */
    public LoginResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        String token = tokenProvider.createToken(principal);
        return new LoginResponse(token, "Bearer", tokenProvider.getExpirationSeconds(),
                userService.me(principal.getId()));
    }
}
