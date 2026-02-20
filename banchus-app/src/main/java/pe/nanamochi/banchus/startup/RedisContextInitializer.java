package pe.nanamochi.banchus.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Slf4j
public class RedisContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  @Override
  public void initialize(ConfigurableApplicationContext context) {
    String host = context.getEnvironment().getProperty("spring.data.redis.host", "localhost");
    int port =
        Integer.parseInt(context.getEnvironment().getProperty("spring.data.redis.port", "6379"));

    LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
    factory.afterPropertiesSet();

    try (var connection = factory.getConnection()) {
      connection.ping();
      log.info("Successfully connected to Redis server.");
    }
  }
}
