package pe.nanamochi.banchus.config;

import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import pe.nanamochi.banchus.redis.model.MultiplayerMatch;
import pe.nanamochi.banchus.redis.model.PacketBundle;

@Configuration
public class RedisConfig {

  @Bean
  public RedisTemplate<String, MultiplayerMatch> multiplayerMatchRedisTemplate(
      RedisConnectionFactory factory) {
    RedisTemplate<String, MultiplayerMatch> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(RedisSerializer.string());
    template.setValueSerializer(RedisSerializer.json());
    return template;
  }

  @Bean
  public RedisTemplate<String, PacketBundle> packetBundleRedisTemplate(
      RedisConnectionFactory factory) {
    RedisTemplate<String, PacketBundle> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(RedisSerializer.string());
    template.setValueSerializer(RedisSerializer.json());
    return template;
  }

  @Bean
  public RedisTemplate<String, UUID> uuidRedisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, UUID> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(RedisSerializer.string());
    template.setValueSerializer(new GenericToStringSerializer<>(UUID.class));
    return template;
  }
}
