package wevioo.se.authms.Dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record MessageResponse(
        String  message,
        Instant timestamp
) {
    public static MessageResponse of(String message) {
        return new MessageResponse(message, Instant.now());
    }
}
