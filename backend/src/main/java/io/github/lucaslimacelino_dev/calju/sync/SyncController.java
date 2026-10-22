package io.github.lucaslimacelino_dev.calju.sync;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.lucaslimacelino_dev.calju.sync.dto.SyncPullResponse;
import io.github.lucaslimacelino_dev.calju.user.CurrentUser;

/** O envio usa as rotas PUT/DELETE de cada recurso; aqui fica só o recebimento incremental. */
@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService service;

    public SyncController(SyncService service) {
        this.service = service;
    }

    /** Primeira vez: cursor=0 (tudo). Depois: o cursor devolvido na resposta anterior. */
    @GetMapping("/pull")
    public SyncPullResponse pull(CurrentUser user, @RequestParam(defaultValue = "0") long cursor,
                                 @RequestParam(defaultValue = "" + SyncService.DEFAULT_LIMIT) int limit) {
        return service.pull(user.id(), cursor, limit);
    }
}
