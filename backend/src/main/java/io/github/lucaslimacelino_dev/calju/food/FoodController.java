package io.github.lucaslimacelino_dev.calju.food;

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
import io.github.lucaslimacelino_dev.calju.food.dto.FoodRequest;
import io.github.lucaslimacelino_dev.calju.food.dto.FoodResponse;
import io.github.lucaslimacelino_dev.calju.user.CurrentUser;

@RestController
@RequestMapping("/api/v1/foods")
public class FoodController {

    private final FoodService service;

    public FoodController(FoodService service) {
        this.service = service;
    }

    /** Só os alimentos ativos; um excluído ainda pode ser lido pelo id. */
    @GetMapping
    public List<FoodResponse> list(CurrentUser user) {
        return service.list(user.id());
    }

    @GetMapping("/{id}")
    public FoodResponse get(CurrentUser user, @PathVariable UUID id) {
        return service.get(user.id(), id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FoodResponse> save(CurrentUser user, @PathVariable UUID id,
                                             @Valid @RequestBody FoodRequest request) {
        Saved<FoodResponse> saved = service.save(user.id(), id, request);
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK).body(saved.value());
    }

    /** Exclusão lógica; {@code version} é a versão que o cliente conhece. */
    @DeleteMapping("/{id}")
    public FoodResponse delete(CurrentUser user, @PathVariable UUID id, @RequestParam int version) {
        return service.delete(user.id(), id, version);
    }
}
