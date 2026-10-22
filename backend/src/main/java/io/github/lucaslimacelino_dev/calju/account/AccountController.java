package io.github.lucaslimacelino_dev.calju.account;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.lucaslimacelino_dev.calju.account.dto.AccountResponse;
import io.github.lucaslimacelino_dev.calju.account.dto.ChangePasswordRequest;
import io.github.lucaslimacelino_dev.calju.account.dto.DeleteAccountRequest;
import io.github.lucaslimacelino_dev.calju.account.dto.SessionResponse;
import io.github.lucaslimacelino_dev.calju.user.CurrentUser;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public AccountResponse account(CurrentUser user) {
        return service.account(user);
    }

    @GetMapping("/sessions")
    public List<SessionResponse> sessions(CurrentUser user) {
        return service.sessions(user);
    }

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endSession(CurrentUser user, @PathVariable UUID id) {
        service.endSession(user, id);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(CurrentUser user, @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(user, request.currentPassword(), request.newPassword());
    }

    /** DELETE com corpo (senha ou confirmação do Google): sem ela, um celular desbloqueado apagaria tudo. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(CurrentUser user, @Valid @RequestBody DeleteAccountRequest request) {
        service.delete(user, request);
    }
}
