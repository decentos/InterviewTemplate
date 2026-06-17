package interview.messages.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitMessageRequest(
        @NotBlank String userId,
        @NotBlank String text
) {
}
