package io.github.lucaslimacelino_dev.calju.goal;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.lucaslimacelino_dev.calju.common.Saved;
import io.github.lucaslimacelino_dev.calju.goal.dto.GoalRequest;
import io.github.lucaslimacelino_dev.calju.goal.dto.GoalResponse;
import io.github.lucaslimacelino_dev.calju.user.CurrentUser;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService service;

    public GoalController(GoalService service) {
        this.service = service;
    }

    @GetMapping
    public List<GoalResponse> list(CurrentUser user) {
        return service.list(user.id()).stream().map(GoalResponse::from).toList();
    }

    @GetMapping("/{id}")
    public GoalResponse get(CurrentUser user, @PathVariable UUID id) {
        return GoalResponse.from(service.get(user.id(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> save(CurrentUser user, @PathVariable UUID id,
                                             @Valid @RequestBody GoalRequest request) {
        Saved<GoalEntity> saved = service.save(user.id(), id, request);
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(GoalResponse.from(saved.value()));
    }
}
