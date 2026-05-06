package wevioo.se.userms.Dto;

import lombok.Builder;
import wevioo.se.userms.Entities.Role;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserSummary(
        UUID id,
        String  username,
        String  email,
        Role role,
        boolean active,
        Instant createdAt
) {}