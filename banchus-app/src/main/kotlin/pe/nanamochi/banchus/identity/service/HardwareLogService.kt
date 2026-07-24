package pe.nanamochi.banchus.identity.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.toResultOr
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.identity.entity.HardwareLog
import pe.nanamochi.banchus.identity.entity.HardwareLogSummary
import pe.nanamochi.banchus.identity.entity.MatchingHardware
import pe.nanamochi.banchus.identity.entity.User
import pe.nanamochi.banchus.identity.repository.HardwareLogRepository
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.HardwareLogNotFound
import pe.nanamochi.banchus.core.util.runDatabaseCatching

@Service
class HardwareLogService(private val hardwareLogRepository: HardwareLogRepository) {
    fun create(hardwareLog: HardwareLog): Result<HardwareLog, DomainMessage> = runDatabaseCatching {
        hardwareLogRepository.save(hardwareLog)
    }

    fun fetchForeignMatchingHardware(
        user: User,
        adapterMd5: String,
        uninstallIdMd5: String,
        diskSignatureMd5: String,
    ): Result<List<MatchingHardware>, DomainMessage> =
        runDatabaseCatching {
                val defaultUninstallIdMd5 = "06a9e146cb8cc0853ded03bb15f2260e"
                val defaultDiskMd5 = "dcfcd07e645d245babe887e5e2daa016"
                val wineAdapterMd5 = "b4ec3c4334a0249dae95c284ec5983df"

                when {
                    uninstallIdMd5 == defaultUninstallIdMd5 ||
                        diskSignatureMd5 == defaultDiskMd5 -> {
                        hardwareLogRepository.fetchForeignStrict(
                            user,
                            adapterMd5,
                            uninstallIdMd5,
                            diskSignatureMd5,
                        )
                    }

                    adapterMd5 == wineAdapterMd5 -> {
                        hardwareLogRepository.fetchForeignByUniqueId(user, uninstallIdMd5)
                    }

                    else -> {
                        hardwareLogRepository.fetchForeignGeneral(
                            user,
                            adapterMd5,
                            uninstallIdMd5,
                            diskSignatureMd5,
                        )
                    }
                }
            }
            .flatMap { list -> if (list.isEmpty()) Err(HardwareLogNotFound) else Ok(list) }

    fun fetchOwnMatchingHardware(
        user: User,
        adapterMd5: String,
        uninstallIdMd5: String,
        diskSignatureMd5: String,
    ): Result<List<HardwareLogSummary>, DomainMessage> =
        hardwareLogRepository
            .fetchOwnMatchingHardware(user, adapterMd5, uninstallIdMd5, diskSignatureMd5)
            .toResultOr { HardwareLogNotFound }
}
