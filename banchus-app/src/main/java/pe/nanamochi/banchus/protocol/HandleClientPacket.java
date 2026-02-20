package pe.nanamochi.banchus.protocol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface HandleClientPacket {
  Packets value();

  boolean checkForRestriction() default false;
}
