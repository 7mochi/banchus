package pe.nanamochi.banchus.services;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.BeatmapRankedStatus;
import pe.nanamochi.banchus.entities.Mode;
import pe.nanamochi.banchus.entities.SubmissionStatus;
import pe.nanamochi.banchus.entities.db.Beatmap;
import pe.nanamochi.banchus.entities.db.Score;
import pe.nanamochi.banchus.entities.db.User;
import pe.nanamochi.banchus.repositories.ScoreRepository;

@Service
public class ScoreService {
  @Autowired private ScoreRepository scoreRepository;

  public Score saveScore(Score score) {
    return scoreRepository.save(score);
  }

  public Score updateScore(Score score) {
    if (!scoreRepository.existsById(score.getId())) {
      throw new IllegalArgumentException("Score not found: " + score.getId());
    }
    return scoreRepository.save(score);
  }

  public List<Score> getMany(Beatmap beatmap, User user, SubmissionStatus submissionStatus) {
    return scoreRepository.findByBeatmapAndUserAndSubmissionStatus(beatmap, user, submissionStatus);
  }

  public List<Score> getMany(
      User user,
      Mode mode,
      String sortBy,
      List<SubmissionStatus> submissionStatuses,
      List<BeatmapRankedStatus> beatmapRankedStatuses,
      int page,
      int pageSize) {
    Sort sort = Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "score");
    Pageable pageable = PageRequest.of(page, pageSize, sort);
    Page<Score> result =
        scoreRepository.findManyFiltered(
            user, mode, submissionStatuses, beatmapRankedStatuses, pageable);
    return result.getContent();
  }

  public int getTotalCount(
      User user,
      Mode mode,
      List<SubmissionStatus> submissionStatuses,
      List<BeatmapRankedStatus> beatmapRankedStatuses) {
    return scoreRepository.countManyFiltered(user, mode, submissionStatuses, beatmapRankedStatuses);
  }

  public Score getBestScore(Beatmap beatmap, User user) {
    List<Score> scores =
        scoreRepository.findTopByBeatmapAndUserAndSubmissionStatusOrderByPerformancePointsDesc(
            beatmap, user, SubmissionStatus.BEST);
    return scores.isEmpty() ? null : scores.getFirst();
  }
}
