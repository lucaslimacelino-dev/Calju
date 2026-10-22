package io.github.lucaslimacelino_dev.calju.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio injetável: os serviços não chamam Instant.now() direto, para os testes controlarem o tempo. */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
