package pe.nanamochi.banchus.commands;

import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.database.entity.User;

@Component
@Command(
    name = "roll",
    documentation = "Roll a random number between 0 and a given number.",
    multiplayer = true)
public class RollCommand extends BaseCommand {
  @Override
  String processCommand(User user, String trigger, String[] args) {
    int max = 100;
    if (args.length != 0) {
      max = Math.min(NumberUtils.toInt(args[0], max), 32767);
    }
    return user.getUsername()
        + " rolls "
        + ThreadLocalRandom.current().nextInt(0, max)
        + " points.";
  }
}
