package io.github.lucaslimacelino_dev.calju.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Refresh token cifrado com AES-GCM, numa chave criada dentro do Android Keystore (a chave nunca
 * sai do sistema de segurança do aparelho; só o texto cifrado fica em SharedPreferences).
 * O arquivo calju_auth fica fora do backup (res/xml/backup_rules e data_extraction_rules).
 */
public class KeystoreSessionStore implements SessionStore {

    static final String PREFS = "calju_auth";
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "calju_session_key";
    private static final String TOKEN = "refresh_token";
    private static final String EMAIL = "email";
    private static final int GCM_TAG_BITS = 128;

    private final SharedPreferences prefs;

    public KeystoreSessionStore(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public synchronized String refreshToken() {
        String stored = prefs.getString(TOKEN, null);
        if (stored == null) {
            return null;
        }
        try {
            String[] parts = stored.split(":", 2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | RuntimeException e) {
            // Chave apagada ou dado corrompido: a sessão é perdida (a pessoa entra de novo).
            clear();
            return null;
        }
    }

    @Override
    public synchronized String email() {
        return prefs.getString(EMAIL, null);
    }

    @Override
    public synchronized void save(String refreshToken, String email) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] encrypted = cipher.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));
            String value = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                    + Base64.encodeToString(encrypted, Base64.NO_WRAP);
            prefs.edit().putString(TOKEN, value).putString(EMAIL, email).apply();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível proteger a sessão", e);
        }
    }

    @Override
    public synchronized void clear() {
        prefs.edit().clear().apply();
    }

    private static SecretKey key() throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            if (keyStore.containsAlias(KEY_ALIAS)) {
                return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
            }
        } catch (IOException e) {
            throw new GeneralSecurityException(e);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
