package pe.nanamochi.banchus.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.HardwareLog
import pe.nanamochi.banchus.database.entity.HardwareLogSummary
import pe.nanamochi.banchus.database.entity.MatchingHardware
import pe.nanamochi.banchus.database.entity.User

@Repository
interface HardwareLogRepository : JpaRepository<HardwareLog, Int> {
    @Query(
        value =
            """
        SELECT
            adapters_md5 AS adaptersMd5,
            uninstall_md5 AS uninstallMd5,
            disk_signature_md5 AS diskSignatureMd5,
            SUM(ocurrencies) AS ocurrencies,
            MAX(activated) AS timestamp,
            MAX(last_used) AS lastUsed
        FROM hardware_logs
        WHERE user_id = :#{#user.id}
          AND (adapters_md5 = :adapterMd5 OR uninstall_md5 = :uninstallIdMd5 OR disk_signature_md5 = :diskSignatureMd5)
        GROUP BY adapters_md5, uninstall_md5, disk_signature_md5
    """,
        nativeQuery = true,
    )
    fun fetchOwnMatchingHardware(
        user: User,
        adapterMd5: String,
        uninstallIdMd5: String,
        diskSignatureMd5: String,
    ): List<HardwareLogSummary>

    @Query(
        value =
            """
        SELECT u.id AS userId, u.username AS username, u.privileges AS userPrivileges,
               hw.adapters_md5 AS adaptersMd5, hw.uninstall_md5 AS uninstallMd5, hw.disk_signature_md5 AS diskSignatureMd5,
               SUM(hw.ocurrencies) AS ocurrencies, MAX(hw.activated) AS timestamp, MAX(hw.last_used) AS lastUsed
        FROM hardware_logs hw
        INNER JOIN users u ON hw.user_id = u.id
        WHERE hw.user_id != :#{#user.id} AND hw.adapters_md5 = :adaptersMd5 AND hw.uninstall_md5 = :uninstallIdMd5 AND hw.disk_signature_md5 = :diskSignatureMd5
        GROUP BY hw.adapters_md5, hw.uninstall_md5, hw.disk_signature_md5, hw.user_id
        ORDER BY hw.user_id
    """,
        nativeQuery = true,
    )
    fun fetchForeignStrict(
        user: User,
        adaptersMd5: String,
        uninstallIdMd5: String,
        diskSignatureMd5: String,
    ): List<MatchingHardware>

    @Query(
        value =
            """
        SELECT u.id AS userId, u.username AS username, u.privileges AS userPrivileges,
               hw.adapters_md5 AS adaptersMd5, hw.uninstall_md5 AS uninstallMd5, hw.disk_signature_md5 AS diskSignatureMd5,
               SUM(hw.ocurrencies) AS ocurrencies, MAX(hw.activated) AS timestamp, MAX(hw.last_used) AS lastUsed
        FROM hardware_logs hw
        INNER JOIN users u ON hw.user_id = u.id
        WHERE hw.user_id != :#{#user.id} AND hw.uninstall_md5 = :uninstallIdMd5
        GROUP BY hw.adapters_md5, hw.uninstall_md5, hw.disk_signature_md5, hw.user_id
        ORDER BY hw.user_id
    """,
        nativeQuery = true,
    )
    fun fetchForeignByUniqueId(user: User, uninstallIdMd5: String): List<MatchingHardware>

    @Query(
        value =
            """
        SELECT u.id AS userId, u.username AS username, u.privileges AS userPrivileges,
               hw.adapters_md5 AS adaptersMd5, hw.uninstall_md5 AS uninstallMd5, hw.disk_signature_md5 AS diskSignatureMd5,
               SUM(hw.ocurrencies) AS ocurrencies, MAX(hw.activated) AS timestamp, MAX(hw.last_used) AS lastUsed
        FROM hardware_logs hw
        INNER JOIN users u ON hw.user_id = u.id
        WHERE hw.user_id != :#{#user.id} AND (hw.adapters_md5 = :adaptersMd5 OR hw.uninstall_md5 = :uninstallIdMd5) AND hw.disk_signature_md5 = :diskSignatureMd5
        GROUP BY hw.adapters_md5, hw.uninstall_md5, hw.disk_signature_md5, hw.user_id
        ORDER BY hw.user_id
    """,
        nativeQuery = true,
    )
    fun fetchForeignGeneral(
        user: User,
        adaptersMd5: String,
        uninstallIdMd5: String,
        diskSignatureMd5: String,
    ): List<MatchingHardware>
}
