package pe.nanamochi.banchus.startup

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.service.StorageService

@Component
class DataDirectoryInitializer(private val storageService: StorageService) : CommandLineRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        runCatching {
                log.info("Initializing storage infrastructure...")
                storageService.initStorage()
            }
            .onSuccess { log.info("Data directories initialized successfully.") }
            .onFailure { e ->
                log.error("Could not initialize data directories: ${e.message}")
                throw IllegalStateException("FileSystem initialization failed", e)
            }
    }
}
