package wevioo.se.userms.Dto;

import lombok.Builder;
import wevioo.se.userms.Entities.Role;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String  username,
        String  email,
        String  firstName,
        String  lastName,
        Role role,
        boolean active,
        String  externalId,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt
) {}
