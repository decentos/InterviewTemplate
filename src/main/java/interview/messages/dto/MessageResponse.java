package interview.messages.dto;

import interview.messages.MessageStatus;

public record MessageResponse(
        String messageId,
        String userId,
        String text,
        MessageStatus status,
        String answer
) {
}
