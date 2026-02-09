package pe.nanamochi.banchus.entities.commons;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubmissionStatus {
  FAILED(0),
  SUBMITTED(1),
  BEST(2);

  private final int value;

  public static SubmissionStatus fromValue(int value) {
    for (SubmissionStatus status : SubmissionStatus.values()) {
      if (status.getValue() == value) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown SubmissionStatus value: " + value);
  }
}
