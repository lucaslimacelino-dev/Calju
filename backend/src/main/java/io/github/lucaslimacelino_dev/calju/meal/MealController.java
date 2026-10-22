package io.github.lucaslimacelino_dev.calju.meal;

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
import io.github.lucaslimacelino_dev.calju.meal.dto.MealRequest;
import io.github.lucaslimacelino_dev.calju.meal.dto.MealResponse;
import io.github.lucaslimacelino_dev.calju.user.CurrentUser;

@RestController
@RequestMapping("/api/v1/meals")
public class MealController {

    private final MealService service;

    public MealController(MealService service) {
        this.service = service;
    }

    @GetMapping
    public List<MealResponse> list(CurrentUser user) {
        return service.list(user.id());
    }

    @GetMapping("/{id}")
    public MealResponse get(CurrentUser user, @PathVariable UUID id) {
        return service.get(user.id(), id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MealResponse> save(CurrentUser user, @PathVariable UUID id,
                                             @Valid @RequestBody MealRequest request) {
        Saved<MealResponse> saved = service.save(user.id(), id, request);
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK).body(saved.value());
    }

    @DeleteMapping("/{id}")
    public MealResponse delete(CurrentUser user, @PathVariable UUID id, @RequestParam int version) {
        return service.delete(user.id(), id, version);
    }
}
