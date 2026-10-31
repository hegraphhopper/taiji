package com.taiji.fund.security;

import com.taiji.fund.common.api.Result;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.security.dto.ChangePasswordRequest;
import com.taiji.fund.security.dto.LoginRequest;
import com.taiji.fund.security.dto.LoginResponse;
import com.taiji.fund.security.dto.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Audit(action = "LOGIN", resourceType = "AUTH")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/logout")
    @Audit(action = "LOGOUT", resourceType = "AUTH")
    public Result<Void> logout() {
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.ok(authService.currentUser());
    }

    @PutMapping("/password")
    @Audit(action = "CHANGE_PASSWORD", resourceType = "AUTH")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.ok();
    }
}
