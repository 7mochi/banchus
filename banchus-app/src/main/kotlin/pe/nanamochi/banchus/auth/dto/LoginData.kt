package pe.nanamochi.banchus.auth.dto

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import pe.nanamochi.banchus.core.error.DecodingRequestFailed
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.UnsupportedClientVersion

enum class ReleaseStream {
    Stable,
    Beta,
    CuttingEdge,
    Tourney,
}

data class OsuVersion(
    val releaseStream: ReleaseStream,
    val versionDate: LocalDate,
    val versionMinor: Int?,
) {
    companion object {
        fun parse(versionString: String): Result<OsuVersion, DomainMessage> {
            if (!versionString.startsWith("b")) return Err(UnsupportedClientVersion)

            return runCatching {
                    val versionString = versionString.removePrefix("b")
                    val (stream, datePart) =
                        when {
                            versionString.endsWith("beta") ->
                                ReleaseStream.Beta to versionString.removeSuffix("beta")
                            versionString.endsWith("cuttingedge") ->
                                ReleaseStream.CuttingEdge to
                                    versionString.removeSuffix("cuttingedge")
                            versionString.endsWith("tourney") ->
                                ReleaseStream.Tourney to versionString.removeSuffix("tourney")
                            else -> ReleaseStream.Stable to versionString
                        }

                    val parts = datePart.split(".")
                    val date = LocalDate.parse(parts[0], DateTimeFormatter.ofPattern("yyyyMMdd"))
                    val minor = parts.getOrNull(1)?.toIntOrNull()

                    OsuVersion(stream, date, minor)
                }
                .mapError { UnsupportedClientVersion }
        }
    }

    fun isOutdated(): Boolean {
        val today = LocalDate.now()
        val expirationMonths =
            when (releaseStream) {
                ReleaseStream.Tourney,
                ReleaseStream.Stable,
                ReleaseStream.Beta -> 24L
                ReleaseStream.CuttingEdge -> 12L
            }

        val expirationDate = versionDate.plusMonths(expirationMonths)
        return today.isAfter(expirationDate)
    }
}

data class ClientInfo(
    val osuVersion: OsuVersion,
    val utcOffset: Int,
    val displayCity: Boolean,
    val clientHashes: ClientHashes,
    val pmPrivate: Boolean,
) {
    companion object {
        fun parse(input: String): Result<ClientInfo, DomainMessage> {
            val parts = input.split("|")
            if (parts.size < 5) return Err(DecodingRequestFailed)

            return binding {
                val version = OsuVersion.parse(parts[0]).bind()
                val hashes = ClientHashes.parse(parts[3]).bind()
                val utcOffset = parts[1].toIntOrNull().toResultOr { DecodingRequestFailed }.bind()

                ClientInfo(
                    osuVersion = version,
                    utcOffset = utcOffset,
                    displayCity = parts[2] == "1",
                    clientHashes = hashes,
                    pmPrivate = parts[4] == "1",
                )
            }
        }
    }
}

data class ClientHashes(
    val osuPathMd5: String,
    val adapters: String,
    val adaptersMd5: String,
    val uninstallMd5: String,
    val diskSignatureMd5: String,
) {
    companion object {
        fun parse(input: String): Result<ClientHashes, DomainMessage> {
            val parts = input.split(":")
            if (parts.size < 5) return Err(DecodingRequestFailed)

            val md5Indices = listOf(0, 2, 3, 4)
            val allHashesValid = md5Indices.all { i -> parts[i].length == 32 }
            if (!allHashesValid) return Err(DecodingRequestFailed)

            return Ok(
                ClientHashes(
                    osuPathMd5 = parts[0],
                    adapters = parts[1],
                    adaptersMd5 = parts[2],
                    uninstallMd5 = parts[3],
                    diskSignatureMd5 = parts[4],
                )
            )
        }
    }
}

data class LoginData(val identifier: String, val secret: String, val clientInfo: ClientInfo) {
    companion object {
        fun parse(raw: String): Result<LoginData, DomainMessage> {
            val lines = raw.split("\n", limit = 3)
            if (lines.size < 3) return Err(DecodingRequestFailed)

            return ClientInfo.parse(lines[2]).map { info ->
                LoginData(identifier = lines[0], secret = lines[1], clientInfo = info)
            }
        }
    }
}
