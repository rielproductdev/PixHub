package com.riel.pixhub.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Conversor JPA que criptografa/descriptografa strings automaticamente
 * ao salvar/ler do banco de dados.
 *
 * == O que é um AttributeConverter? ==
 * É um "tradutor" entre o Java e o banco. O JPA chama:
 * - convertToDatabaseColumn(): ANTES de salvar (Java → banco)
 * - convertToEntityAttribute(): DEPOIS de ler (banco → Java)
 *
 * O código Java trabalha com texto puro ("12345678901"),
 * mas no banco fica criptografado ("aG9sZGVyRG9jdW1lbnQ=...").
 * Transparente para o restante da aplicação.
 *
 * == Algoritmo: AES-256-GCM ==
 *
 * AES (Advanced Encryption Standard):
 * - Criptografia simétrica: mesma chave para cifrar e decifrar
 * - "256" = tamanho da chave em bits (32 bytes)
 * - Padrão mundial aprovado pelo NIST, usado por bancos e governos
 *
 * GCM (Galois/Counter Mode):
 * - Modo de operação que combina CRIPTOGRAFIA + AUTENTICAÇÃO
 * - Gera um "authentication tag" (128 bits) junto com o ciphertext
 * - Se alguém alterar 1 bit do dado criptografado no banco,
 *   o GCM detecta e lança AEADBadTagException ao descriptografar
 * - Isso protege contra ataques de "tampering" (adulteração)
 *
 * Comparação com outros modos:
 * ┌──────────┬──────────────┬──────────────┬─────────────────┐
 * │ Modo     │ Criptografa  │ Autentica    │ Precisa de IV?  │
 * ├──────────┼──────────────┼──────────────┼─────────────────┤
 * │ ECB      │ ✓            │ ✗            │ Não (inseguro!) │
 * │ CBC      │ ✓            │ ✗            │ Sim             │
 * │ GCM      │ ✓            │ ✓            │ Sim             │
 * └──────────┴──────────────┴──────────────┴─────────────────┘
 * GCM é o padrão recomendado para dados em repouso (at-rest encryption).
 *
 * == Formato do dado no banco ==
 * Base64( IV (12 bytes) + ciphertext + authTag (16 bytes) )
 *
 * O IV (Initialization Vector) é concatenado ao ciphertext porque
 * precisamos dele para descriptografar, e ele NÃO é segredo — apenas
 * precisa ser ÚNICO para cada operação de criptografia.
 *
 * == Por que IV aleatório a cada escrita? ==
 * Se dois CPFs iguais usassem o mesmo IV, produziriam o MESMO ciphertext.
 * Um atacante que visse dois campos iguais no banco saberia que são o mesmo CPF,
 * mesmo sem saber qual. IV aleatório garante que cada criptografia é diferente.
 *
 * @Converter: Registra no JPA como conversor disponível
 * @Component: Spring gerencia o ciclo de vida (permite injetar EncryptionProperties)
 */
@Converter
@Component
@Slf4j
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    /**
     * Algoritmo completo: AES com GCM e sem padding.
     * GCM não precisa de padding porque opera como stream cipher internamente.
     */
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /**
     * Tamanho do IV (Initialization Vector) em bytes.
     * 12 bytes (96 bits) é o tamanho recomendado pelo NIST para AES-GCM.
     * Outros tamanhos funcionam, mas 12 é o mais eficiente.
     */
    private static final int IV_LENGTH = 12;

    /**
     * Tamanho do authentication tag em bits.
     * 128 bits (16 bytes) é o máximo e mais seguro.
     * O tag é gerado automaticamente pelo GCM e anexado ao ciphertext.
     */
    private static final int TAG_LENGTH = 128;

    /**
     * Gerador de números aleatórios criptograficamente seguro.
     * SecureRandom usa entropia do sistema operacional (/dev/urandom no Linux).
     * Thread-safe — pode ser compartilhado entre threads.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey secretKey;

    /**
     * Construtor — recebe a chave de criptografia via injeção de dependência.
     *
     * O Spring injeta EncryptionProperties (que lê do application.yml).
     * A chave vem em Base64 e é decodificada para bytes.
     *
     * SecretKeySpec: cria uma chave AES a partir dos bytes brutos.
     * Valida que a chave tem exatamente 32 bytes (256 bits).
     */
    public EncryptedStringConverter(EncryptionProperties encryptionProperties) {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionProperties.getKey());

        // Validação de segurança: AES-256 exige chave de 32 bytes
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "Chave de criptografia deve ter 32 bytes (256 bits). " +
                    "Recebido: " + keyBytes.length + " bytes. " +
                    "Gere uma nova com: openssl rand -base64 32");
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("EncryptedStringConverter inicializado com sucesso (AES-256-GCM)");
    }

    /**
     * Criptografa o valor ANTES de salvar no banco.
     *
     * Fluxo:
     * 1. Gera IV aleatório de 12 bytes (único para cada operação)
     * 2. Configura o Cipher com a chave + IV
     * 3. Criptografa o texto → produz ciphertext + authTag
     * 4. Concatena: IV + ciphertext + authTag
     * 5. Codifica tudo em Base64 (banco armazena como VARCHAR)
     *
     * Exemplo visual:
     *   "12345678901" (CPF)
     *   → cifrar com AES-256-GCM
     *   → IV (12 bytes) + ciphertext (11 bytes) + tag (16 bytes) = 39 bytes
     *   → Base64 → "dGhpcyBpcyBhbiBleGFtcGxl..." (52 chars)
     *
     * @param attribute valor em texto puro (ex: "12345678901")
     * @return valor criptografado em Base64
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            // 1. Gerar IV aleatório — NUNCA reutilizar IV com a mesma chave
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            // 2. Configurar o Cipher (motor de criptografia)
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 3. Criptografar — doFinal retorna ciphertext + authTag juntos
            byte[] encryptedBytes = cipher.doFinal(attribute.getBytes());

            // 4. Concatenar IV + ciphertext+tag em um único array
            //    Precisamos do IV para descriptografar, então guardamos junto
            ByteBuffer byteBuffer = ByteBuffer.allocate(IV_LENGTH + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            // 5. Base64 para armazenar como texto no banco (VARCHAR)
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar dado sensível", e);
        }
    }

    /**
     * Descriptografa o valor DEPOIS de ler do banco.
     *
     * Fluxo inverso:
     * 1. Decodifica o Base64 → bytes
     * 2. Extrai o IV (primeiros 12 bytes)
     * 3. Extrai o ciphertext+authTag (restante)
     * 4. Configura o Cipher em modo DECRYPT com a chave + IV
     * 5. Descriptografa e VERIFICA o authTag
     *
     * Se o authTag não bater (dado foi alterado no banco),
     * o Cipher lança AEADBadTagException — proteção contra adulteração.
     *
     * @param dbData valor criptografado em Base64
     * @return valor em texto puro (ex: "12345678901")
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        try {
            // 1. Decodificar Base64 → bytes brutos
            byte[] decoded = Base64.getDecoder().decode(dbData);

            // 2. Separar IV e ciphertext+tag usando ByteBuffer
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            // Extrair IV (primeiros 12 bytes)
            byte[] iv = new byte[IV_LENGTH];
            byteBuffer.get(iv);

            // Extrair ciphertext + authTag (tudo que sobrou)
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            // 3. Configurar o Cipher em modo DECRYPT
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 4. Descriptografar — também valida o authTag
            //    Se o dado foi adulterado, lança AEADBadTagException aqui
            byte[] decryptedBytes = cipher.doFinal(ciphertext);

            return new String(decryptedBytes);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao descriptografar dado sensível. " +
                    "Possíveis causas: chave de criptografia alterada ou dado corrompido", e);
        }
    }
}
