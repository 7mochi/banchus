package pe.nanamochi.banchus.domain.error

import pe.nanamochi.banchus.database.entity.User

sealed interface DomainMessage

data class DatabaseError(val reason: String?) : DomainMessage

data object InternalError : DomainMessage

sealed interface BanchoError : DomainMessage

data object DecodingRequestFailed : DomainMessage

data object UnsupportedClientVersion : DomainMessage

data object ClientTooOld : DomainMessage

data object InvalidToken : BanchoError

sealed interface RegistrationResult

data class UserCreated(val user: User) : RegistrationResult

data object CheckOk : RegistrationResult

sealed interface RegistrationError : DomainMessage

data object UsernameTaken : RegistrationError

data object EmailTaken : RegistrationError

sealed class InvalidFormat(val field: String, val message: String) : RegistrationError

data object InvalidUsername : InvalidFormat("username", "Invalid username format.")

data object InvalidEmail : InvalidFormat("user_email", "Invalid email syntax.")

data object InvalidPassword : InvalidFormat("password", "Password is too weak.")

sealed interface UserError : DomainMessage

data object UserNotFound : UserError

data object UserSilenced : UserError

data object UserRestricted : UserError

data object InvalidCredentials : UserError

data object InteractionBlocked : MessageError

sealed interface MultiplayerError : DomainMessage

data object MatchNotFound : MultiplayerError

data object MultiplayerUnauthorized : MultiplayerError

data object NotInMatch : MultiplayerError

data object MultiplayerMatchFull : MultiplayerError

data object SlotNotFound : MultiplayerError

data object IncorrectPassword : MultiplayerError

sealed interface SpectatorError : DomainMessage

data object InvalidSpectateTarget : SpectatorError

sealed interface CommandError : DomainMessage

data object CommandNotFound : CommandError

sealed interface StatError : DomainMessage

data object StatNotFound : StatError

sealed interface HardwareLogError : DomainMessage

data object HardwareLogNotFound : HardwareLogError

sealed interface RelationshipError : DomainMessage

data object RelationshipNotFound : RelationshipError

sealed interface SessionError : DomainMessage

data object SessionNotFound : SessionError

data class SessionExpired(val payload: ByteArray) : SessionError

data object SessionInvalidCredentials : UserError

data object SessionsLimitReached : UserError

sealed interface ScoreError : DomainMessage

data object ScoreNotFound : ScoreError

sealed interface ChannelError : DomainMessage

data object ChannelNotFound : ChannelError

data object ChannelIsUnauthorized : ChannelError

data object ChannelUserAlreadyIn : ChannelError

sealed interface MessageError : DomainMessage

data object MessageUserAutoSilenced : MessageError

data object MessageInvalidLength : MessageError

sealed interface BeatmapError : DomainMessage

data object BeatmapNotFound : BeatmapError

sealed interface BeatmapsetError : DomainMessage

data object BeatmapsetNotFound : BeatmapsetError

sealed interface StorageError : DomainMessage

data class StorageWriteError(val cause: String?) : StorageError

data object FileNotFound : StorageError

sealed interface PerformanceError : DomainMessage

data object CalculatorNotFound : PerformanceError

data object CalculationFailed : PerformanceError

sealed interface GeolocationError : DomainMessage

data object ResolutionFailed : GeolocationError

sealed interface SilenceError : DomainMessage

data object SelfSilenceNotAllowed : SilenceError

data object BotSilenceNotAllowed : SilenceError

data object InvalidDuration : SilenceError

sealed interface AuditLogError : DomainMessage

data object AuditLogNotFound : AuditLogError
