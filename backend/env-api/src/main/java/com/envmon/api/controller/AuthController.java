package com.envmon.api.controller;

import com.envmon.common.audit.Audit;
import com.envmon.service.AuthService;
import com.envmon.service.dto.LoginRequest;
import com.envmon.service.dto.LoginResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Audit(module = "auth", action = "LOGIN")
    public LoginResult login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/profile")
    public LoginResult.UserInfo profile() {
        return authService.currentUser();
    }
}
