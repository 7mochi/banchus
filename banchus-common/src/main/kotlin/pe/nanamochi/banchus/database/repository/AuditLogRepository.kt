package pe.nanamochi.banchus.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.AuditLog

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    fun findAuditLogById(id: Long): AuditLog?
}
