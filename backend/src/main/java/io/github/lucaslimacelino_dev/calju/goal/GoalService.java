package io.github.lucaslimacelino_dev.calju.goal;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lucaslimacelino_dev.calju.common.ConflictException;
import io.github.lucaslimacelino_dev.calju.common.NotFoundException;
import io.github.lucaslimacelino_dev.calju.common.Saved;
import io.github.lucaslimacelino_dev.calju.common.Versions;
import io.github.lucaslimacelino_dev.calju.goal.dto.GoalRequest;
import io.github.lucaslimacelino_dev.calju.user.UserRepository;

/** Regras das metas. Toda leitura e escrita filtra pelo dono; meta de outro usuário = 404. */
@Service
public class GoalService {

    private static final String NOT_FOUND = "Meta não encontrada.";

    private final GoalRepository goals;
    private final UserRepository users;
    private final Clock clock;

    public GoalService(UserRepository users, GoalRepository goals, Clock clock) {
        this.users = users;
        this.goals = goals;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<GoalEntity> list(UUID ownerId) {
        return goals.findByOwnerIdOrderByEffectiveFrom(ownerId);
    }

    @Transactional(readOnly = true)
    public GoalEntity get(UUID ownerId, UUID id) {
        return goals.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    /**
     * Cria (id novo, versão 0) ou atualiza (versão igual à atual). O id é gerado no aparelho,
     * então repetir a mesma requisição de criação não duplica a meta.
     */
    @Transactional
    public Saved<GoalEntity> save(UUID ownerId, UUID id, GoalRequest request) {
        users.lockForWrite(ownerId);
        Optional<GoalEntity> existing = goals.findForUpdate(id);
        GoalEntity goal;
        boolean created = existing.isEmpty();
        if (created) {
            if (request.version() != Versions.NEW) {
                throw new NotFoundException(NOT_FOUND);
            }
            goal = new GoalEntity(id, ownerId);
        } else {
            goal = existing.get();
            if (!goal.getOwnerId().equals(ownerId)) {
                throw new NotFoundException(NOT_FOUND);
            }
            Versions.requireCurrent(request.version(), goal.getVersion());
        }
        if (goals.existsByOwnerIdAndEffectiveFromAndIdNot(ownerId, request.effectiveFrom(), id)) {
            throw new ConflictException("Já existe uma meta que começa em " + request.effectiveFrom() + ".");
        }
        goal.apply(request.effectiveFrom(), request.kcal(), request.proteinG(), request.carbohydrateG(),
                request.fatG(), clock.instant());
        return new Saved<>(goals.save(goal), created);
    }
}
