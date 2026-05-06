package wevioo.se.userms.Dto;

import java.util.UUID;

public record UserCredentialsResponse(
        UUID id,
        String  username,
        String  email,
        String  passwordHash,
        String  role,
        boolean active
) {}
