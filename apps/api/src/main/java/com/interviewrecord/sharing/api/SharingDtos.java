package com.interviewrecord.sharing.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SharingDtos {
    private SharingDtos() {}

    public record RoundSelection(@NotBlank String roundId, @NotNull Set<@NotBlank String> visibleFields) {}
    public record CreateShareRequest(@NotNull Set<@NotBlank String> positionFields,
            @NotNull List<@Valid RoundSelection> rounds,
            @NotBlank String expiry) {}
    public record CreatedShareResponse(String id, String token, Instant expiresAt, String publicPath) {}
    public record ShareLinkResponse(String id, Set<String> positionFields, List<RoundSelection> rounds,
            Instant expiresAt, Instant revokedAt, Instant createdAt) {}
    public record PublicRoundResponse(String id, Map<String, Object> content) {}
    public record PublicShareResponse(Map<String, Object> position, List<PublicRoundResponse> rounds,
            String robots) {}
}
