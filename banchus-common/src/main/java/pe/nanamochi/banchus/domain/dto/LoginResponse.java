package pe.nanamochi.banchus.domain.dto;

public record LoginResponse(String token, byte[] payload, boolean success) {}
