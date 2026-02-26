package pe.nanamochi.banchus.domain.errors

import pe.nanamochi.banchus.database.entity.User

sealed interface DomainMessage

data class DatabaseError(val reason: String?) : DomainMessage

data object InternalError : DomainMessage

sealed interface BanchoError : DomainMessage

data object InvalidLoginFormat : BanchoError

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

data object InvalidCredentials : UserError

sealed interface MultiplayerError : DomainMessage

data object MatchNotFound : MultiplayerError

data object SlotNotFound : MultiplayerError

sealed interface StatError : DomainMessage

data object StatNotFound : StatError

sealed interface SessionError : DomainMessage

data object SessionNotFound : SessionError

sealed interface ScoreError : DomainMessage

data object ScoreNotFound : ScoreError

sealed interface ChannelError : DomainMessage

data object ChannelNotFound : ChannelError

data object ChannelInsufficientPrivileges : ChannelError

data object ChannelUserAlreadyIn : ChannelError

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
