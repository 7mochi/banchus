package pe.nanamochi.banchus.service;

import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.database.entity.User;
import pe.nanamochi.banchus.database.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;

  public Optional<User> login(String username, String passwordMd5) {
    return userRepository.findByUsernameAndPasswordMd5(username, passwordMd5);
  }

  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  @Transactional
  public User create(User user) {
    return userRepository.save(user);
  }

  @Transactional
  public User update(User user) {
    if (!userRepository.existsById(user.getId())) {
      throw new IllegalArgumentException("User not found: " + user.getUsername());
    }
    return userRepository.save(user);
  }
}
