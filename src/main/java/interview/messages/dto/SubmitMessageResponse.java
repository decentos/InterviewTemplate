package interview.messages.dto;

import interview.messages.MessageStatus;

public record SubmitMessageResponse(
        String messageId,
        MessageStatus status
) {
}
