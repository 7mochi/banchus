package pe.nanamochi.banchus

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import pe.nanamochi.banchus.startup.RedisContextInitializer

@SpringBootApplication @EnableJpaAuditing class BanchusNewApplication

fun main(args: Array<String>) {
    runApplication<BanchusNewApplication>(*args) { addInitializers(RedisContextInitializer()) }
}
