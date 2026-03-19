package com.riel.pixhub.repository;

import com.riel.pixhub.entity.QrCode;
import com.riel.pixhub.enums.QrCodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para a entidade QrCode.
 */
@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {

    /** Busca QR Code pelo txId (para conciliar pagamento com cobrança) */
    Optional<QrCode> findByTxId(String txId);

    /** Lista QR Codes vinculados a uma chave PIX */
    List<QrCode> findByPixKey(String pixKey);

    /** Filtra por status (ex: buscar QRs ACTIVE que precisam ser verificados se expiraram) */
    List<QrCode> findByStatus(QrCodeStatus status);
}
