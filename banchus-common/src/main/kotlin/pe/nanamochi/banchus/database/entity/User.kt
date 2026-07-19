package pe.nanamochi.banchus.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.ServerPrivileges

@Entity
@Table(
    name = "users",
    indexes = [Index(name = "idx_users_safe_name", columnList = "safe_username")],
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    var id: Int = 0,
    @Column(name = "username", length = 32, nullable = false, unique = true)
    var username: String = "",
    @Column(name = "safe_username", length = 32, nullable = false, unique = true)
    var safeUsername: String = "",
    @Column(name = "email", length = 64, nullable = false, unique = true) var email: String = "",
    @Column(name = "password_bcrypt", length = 60, nullable = false)
    var passwordBcrypt: String = "",
    @Column(name = "registration_time", nullable = false)
    var registrationTime: Instant = Instant.now(),
    @Column(name = "latest_activity", nullable = false) var latestActivity: Instant = Instant.now(),
    @Column(name = "country", length = 2, nullable = false)
    var country: CountryCode = CountryCode.XX,
    @Column(name = "silence_end", nullable = true) var silenceEnd: Instant? = null,
    @Column(name = "restriction_time", nullable = true) var restrictionTime: Instant? = null,
    @Column(name = "privileges", nullable = false) var privileges: Int = 1,
) {
    val isSilenced: Boolean
        get() = silenceEnd?.isAfter(Instant.now()) ?: false

    val isRestricted: Boolean
        get() = !ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.UNRESTRICTED)

    val isDonor: Boolean
        get() = ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.DONATOR)

    val isStaff: Boolean
        get() = ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.CHAT_MODERATOR)

    val isDeveloper: Boolean
        get() = ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.SUPER_ADMIN)

    val isTournamentStaff: Boolean
        get() = ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.TOURNAMENT_STAFF)
}
