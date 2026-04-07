package hexlet.code.app.component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "rsa")
@Getter
@Setter
public class RsaKeyProperties {
    private String privateKey;
    private String publicKey;

    private RSAPublicKey rsaPublicKey;
    private RSAPrivateKey rsaPrivateKey;

    @PostConstruct
    public void loadKeys() throws Exception {
        if (publicKey != null && publicKey.startsWith("classpath:")) {
            String pubKeyPath = publicKey.replace("classpath:", "");
            String pubKeyContent = loadKeyFile(pubKeyPath);
            rsaPublicKey = parsePublicKey(pubKeyContent);
        }
        if (privateKey != null && privateKey.startsWith("classpath:")) {
            String privKeyPath = privateKey.replace("classpath:", "");
            String privKeyContent = loadKeyFile(privKeyPath);
            rsaPrivateKey = parsePrivateKey(privKeyContent);
        }
    }

    private String loadKeyFile(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (var is = resource.getInputStream();
             var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(java.util.stream.Collectors.joining("\n"));
        }
    }

    private RSAPublicKey parsePublicKey(String keyContent) throws Exception {
        String publicKeyPEM = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    private RSAPrivateKey parsePrivateKey(String keyContent) throws Exception {
        String privateKeyPEM = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }
}
