package io.github.lucaslimacelino_dev.calju.user;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /** O e-mail é guardado em minúsculas, sem espaços (ver Emails.normalize). */
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByGoogleSubject(String googleSubject);

    /**
     * Trava a linha do usuário até o fim da transação. Toda gravação de dados chama isto antes,
     * para as mudanças de um usuário receberem números de sequência em ordem (ver V8).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u.id from UserEntity u where u.id = :id")
    Optional<UUID> lockForWrite(UUID id);
}
