package pe.nanamochi.banchus.core.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.core.entity.AuditLog

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    fun findAuditLogById(id: Long): AuditLog?
}
