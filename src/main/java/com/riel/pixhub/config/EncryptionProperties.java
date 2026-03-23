package com.riel.pixhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

/**
 * Propriedades de configuração para criptografia de dados sensíveis.
 *
 * Lê a chave AES-256 do application.yml (seção "encryption").
 * Em produção, a chave DEVE vir de variável de ambiente:
 *   ENCRYPTION_KEY=<chave Base64 de 32 bytes>
 *
 * Por que AES-256-GCM?
 * - AES: padrão da indústria, aprovado pelo NIST
 * - 256 bits: nível de segurança mais alto do AES
 * - GCM (Galois/Counter Mode): além de criptografar, AUTENTICA os dados.
 *   Se alguém alterar o dado criptografado no banco, o GCM detecta a alteração
 *   e lança exceção (integridade garantida). Modos mais antigos como CBC não fazem isso.
 *
 * Formato da chave:
 * - Deve ser uma string Base64 que decodifica em exatamente 32 bytes (256 bits).
 * - Exemplo de geração:
 *   openssl rand -base64 32
 *   → gera algo como "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3=="
 *
 * @ConfigurationProperties: Spring lê automaticamente do YAML e popula os campos.
 *   encryption.key no YAML → this.key
 */
@Component
@ConfigurationProperties(prefix = "encryption")
@Getter
@Setter
public class EncryptionProperties {

    /**
     * Chave AES-256 em formato Base64.
     *
     * Valor padrão é apenas para desenvolvimento local.
     * NUNCA usar o valor padrão em produção — variável de ambiente obrigatória.
     */
    private String key;
}
