package interview.messages.model;

import interview.messages.MessageStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;

@RedisHash("llm_messages")
public class RedisMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String messageId;

    @Indexed
    private String userId;

    private String text;
    private MessageStatus status;
    private String answer;

    public RedisMessage() {
    }

    public RedisMessage(String messageId, String userId, String text, MessageStatus status, String answer) {
        this.messageId = messageId;
        this.userId = userId;
        this.text = text;
        this.status = status;
        this.answer = answer;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
