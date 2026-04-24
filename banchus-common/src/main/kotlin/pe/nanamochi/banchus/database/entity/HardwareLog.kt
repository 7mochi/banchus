package pe.nanamochi.banchus.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "hardware_logs")
class HardwareLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") var user: User? = null,
    @Column(name = "adapters_md5", length = 32, nullable = false) var adaptersMd5: String = "",
    @Column(name = "uninstall_md5", length = 32, nullable = false) var uninstallMd5: String = "",
    @Column(name = "disk_signature_md5", length = 32, nullable = false)
    var diskSignatureMd5: String = "",
    @Column(name = "ocurrencies", nullable = false) var ocurrencies: Int = 0,
    @Column(name = "activated", nullable = false) var timestamp: Long = 0L,
    @Column(name = "last_used") var lastUsed: Instant = Instant.now(),
)

interface HardwareLogSummary {
    fun getAdaptersMd5(): String

    fun getUninstallMd5(): String

    fun getDiskSignatureMd5(): String

    fun getOcurrencies(): Long

    fun getTimestamp(): Long

    fun getLastUsed(): Instant
}

interface MatchingHardware : HardwareLogSummary {
    fun getUserId(): Int

    fun getUsername(): String

    fun getUserPrivileges(): Int
}
