package pe.nanamochi.banchus.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import pe.nanamochi.banchus.protocol.PacketReader
import pe.nanamochi.banchus.protocol.PacketWriter

@Configuration
class AppConfig {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    fun packetWriter(): PacketWriter {
        return PacketWriter()
    }

    @Bean
    fun packetReader(): PacketReader {
        return PacketReader()
    }
}
