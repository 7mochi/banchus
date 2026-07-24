package pe.nanamochi.banchus.score.dto

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.InternalError

data class DecryptedScoreData(
    val beatmapMd5: String,
    val username: String,
    val onlineChecksum: String,
    val n300: Int,
    val n100: Int,
    val n50: Int,
    val nGeki: Int,
    val nKatu: Int,
    val nMiss: Int,
    val score: Int,
    val highestCombo: Int,
    val fullCombo: Boolean,
    val grade: String,
    val mods: Int,
    val passed: Boolean,
    val mode: Int,
) {
    companion object {
        fun fromTokens(tokens: List<String>): Result<DecryptedScoreData, DomainMessage> {
            if (tokens.size < 16) {
                return Err(InternalError)
            }

            return runCatching {
                    DecryptedScoreData(
                        beatmapMd5 = tokens[0],
                        username = tokens[1].trim(),
                        onlineChecksum = tokens[2],
                        n300 = tokens[3].toInt(),
                        n100 = tokens[4].toInt(),
                        n50 = tokens[5].toInt(),
                        nGeki = tokens[6].toInt(),
                        nKatu = tokens[7].toInt(),
                        nMiss = tokens[8].toInt(),
                        score = tokens[9].toInt(),
                        highestCombo = tokens[10].toInt(),
                        fullCombo =
                            tokens[11] == "1" || tokens[11].equals("True", ignoreCase = true),
                        grade = tokens[12],
                        mods = tokens[13].toInt(),
                        passed = tokens[14] == "1" || tokens[14].equals("True", ignoreCase = true),
                        mode = tokens[15].toInt(),
                    )
                }
                .mapError { InternalError }
        }
    }
}
