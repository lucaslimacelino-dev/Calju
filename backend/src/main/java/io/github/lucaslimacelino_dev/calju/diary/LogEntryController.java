package io.github.lucaslimacelino_dev.calju.diary;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.lucaslimacelino_dev.calju.common.Saved;
import io.github.lucaslimacelino_dev.calju.diary.dto.LogEntryRequest;
import io.github.lucaslimacelino_dev.calju.diary.dto.LogEntryResponse;
import io.github.lucaslimacelino_dev.calju.user.CurrentUser;

@RestController
@RequestMapping("/api/v1/log-entries")
public class LogEntryController {

    private final LogEntryService service;

    public LogEntryController(LogEntryService service) {
        this.service = service;
    }

    /** Registros ativos de um período, ex.: ?from=2026-10-01&amp;to=2026-10-22 (no máximo 366 dias). */
    @GetMapping
    public List<LogEntryResponse> range(CurrentUser user, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.range(user.id(), from, to);
    }

    @GetMapping("/{id}")
    public LogEntryResponse get(CurrentUser user, @PathVariable UUID id) {
        return service.get(user.id(), id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LogEntryResponse> save(CurrentUser user, @PathVariable UUID id,
                                                 @Valid @RequestBody LogEntryRequest request) {
        Saved<LogEntryResponse> saved = service.save(user.id(), id, request);
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK).body(saved.value());
    }

    @DeleteMapping("/{id}")
    public LogEntryResponse delete(CurrentUser user, @PathVariable UUID id, @RequestParam int version) {
        return service.delete(user.id(), id, version);
    }
}
