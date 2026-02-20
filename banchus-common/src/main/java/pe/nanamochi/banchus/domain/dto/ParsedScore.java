package pe.nanamochi.banchus.domain.dto;

import pe.nanamochi.banchus.database.entity.Score;

public record ParsedScore(Score score, byte[] replayBytes, String beatmapMd5, String username) {}
