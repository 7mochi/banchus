package pe.nanamochi.banchus.config;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.web.client.RestTemplate;
import pe.nanamochi.banchus.core.ClientPacket;
import pe.nanamochi.banchus.protocol.PacketReader;
import pe.nanamochi.banchus.protocol.PacketWriter;

@Slf4j
@Configuration
public class AppConfig {
  @Bean
  public PacketWriter packetWriter() {
    return new PacketWriter();
  }

  @Bean
  public PacketReader packetReader() {
    List<ClientPacket> prototypes = new ArrayList<>();
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AssignableTypeFilter(ClientPacket.class));

    String basePackage = "pe.nanamochi.banchus.packets.client";
    for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
      try {
        Class<?> clazz = Class.forName(bd.getBeanClassName());
        ClientPacket packet = (ClientPacket) clazz.getDeclaredConstructor().newInstance();
        prototypes.add(packet);
      } catch (Exception e) {
        log.error("Failed to load packet class: {}", bd.getBeanClassName(), e);
      }
    }

    log.info("Loaded {} client packet prototypes.", prototypes.size());
    return new PacketReader(prototypes);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
