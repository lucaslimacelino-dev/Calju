package io.github.lucaslimacelino_dev.calju.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    private static String base64OfBytes(int size) {
        return Base64.getEncoder().encodeToString(new byte[size]);
    }

    @Test
    void segredoDe32BytesViraChaveHmacSha256() {
        assertThat(new JwtProperties(base64OfBytes(32), "calju").secretKey().getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void segredoCurtoDemaisEhRecusado() {
        assertThatThrownBy(() -> new JwtProperties(base64OfBytes(16), "calju").secretKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void segredoQueNaoEhBase64EhRecusado() {
        assertThatThrownBy(() -> new JwtProperties("isto não é base64!", "calju").secretKey())
                .isInstanceOf(IllegalStateException.class);
    }
}
