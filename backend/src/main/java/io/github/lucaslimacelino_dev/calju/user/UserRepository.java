package io.github.lucaslimacelino_dev.calju.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /** O e-mail é guardado em minúsculas, sem espaços (ver Emails.normalize). */
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByGoogleSubject(String googleSubject);
}
