package com.sipm.auth;

import com.sipm.auth.dto.LoginRequest;
import com.sipm.auth.dto.LoginResponse;
import com.sipm.auth.dto.ThemeRequest;
import com.sipm.security.AppUserPrincipal;
import com.sipm.user.UserService;
import com.sipm.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal me) {
        return userService.me(me.getId());
    }

    /** 내 화면 스킨(낮/밤) 저장. 로그인한 본인 것만 바꿀 수 있다. */
    @PutMapping("/me/theme")
    public UserResponse updateTheme(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody ThemeRequest req) {
        return userService.updateTheme(me.getId(), req.theme());
    }
}
