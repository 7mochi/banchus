package pe.nanamochi.banchus.core.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.core.entity.AuditLog
import pe.nanamochi.banchus.core.error.AuditLogNotFound
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.repository.AuditLogRepository
import pe.nanamochi.banchus.core.util.runDatabaseCatching

@Service
class AuditLogService(private val auditLogRepository: AuditLogRepository) {
    fun create(auditLog: AuditLog): Result<AuditLog, DomainMessage> = runDatabaseCatching {
        auditLogRepository.save(auditLog)
    }

    fun update(auditLog: AuditLog): Result<AuditLog, DomainMessage> =
        if (auditLogRepository.existsById(auditLog.id)) {
            runDatabaseCatching { auditLogRepository.save(auditLog) }
        } else {
            Err(AuditLogNotFound)
        }

    fun fetchOneById(id: Long): Result<AuditLog, DomainMessage> =
        auditLogRepository.findAuditLogById(id).toResultOr { AuditLogNotFound }
}
