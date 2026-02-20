package pe.nanamochi.banchus.service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.database.repository.SessionRepository;

@Service
@RequiredArgsConstructor
public class SessionService {
  private final SessionRepository sessionRepository;

  public Optional<Session> findById(UUID id) {
    return sessionRepository.findById(id);
  }

  public List<Session> findAll() {
    return sessionRepository.findAll();
  }

  public Optional<Session> findPrimaryByUsername(String username) {
    return sessionRepository.findByUser_UsernameIgnoreCaseAndPrimarySessionTrue(username);
  }

  public Optional<Session> findPrimaryByUserId(int userId) {
    return sessionRepository.findByUser_IdAndPrimarySessionTrue(userId);
  }

  @Transactional
  public Session create(Session session) {
    if (session.isPrimarySession()) {
      sessionRepository
          .findByUser_IdAndPrimarySessionTrue(session.getUser().getId())
          .ifPresent(
              oldPrimary -> {
                oldPrimary.setPrimarySession(false);
                sessionRepository.save(oldPrimary);
              });
    }
    return sessionRepository.save(session);
  }

  @Transactional
  public Session update(Session session) {
    if (!sessionRepository.existsById(session.getId())) {
      throw new IllegalArgumentException("Session not found: " + session.getId());
    }
    return sessionRepository.save(session);
  }

  @Transactional
  public Session delete(Session session) {
    if (!sessionRepository.existsById(session.getId())) {
      throw new IllegalArgumentException("Session not found: " + session.getId());
    }
    sessionRepository.delete(session);
    return session;
  }
}
