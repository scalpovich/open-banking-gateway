package de.adorsys.opba.protocol.facade.config;

import com.google.common.io.Resources;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import lombok.SneakyThrows;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;
import java.security.Provider;
import java.security.Security;

@Configuration
public class FacadeConfig {
    @Bean
    @SneakyThrows
    public Aead systemAeadConfig() {
        AeadConfig.register();
        String path = Paths.get(Resources.getResource("keyset.json").toURI()).toAbsolutePath().toString();
        KeysetHandle systemKeysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withPath(path));
        return systemKeysetHandle.getPrimitive(Aead.class);
    }

    @Bean
    public Provider securityProvider() {
        if (null == Security.getProperty(BouncyCastleProvider.PROVIDER_NAME)) {
            Security.addProvider(new BouncyCastleProvider());
        }

        return Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
    }
}