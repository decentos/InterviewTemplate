package interview.messages;

import interview.messages.dto.MessageResponse;
import interview.messages.dto.SubmitMessageRequest;
import interview.messages.dto.SubmitMessageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<SubmitMessageResponse> submit(@Valid @RequestBody SubmitMessageRequest request) {
        SubmitMessageResponse response = messageService.submit(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{messageId}")
                .buildAndExpand(response.messageId())
                .toUri();

        return ResponseEntity.accepted()
                .location(location)
                .body(response);
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponse> findById(@PathVariable("messageId") String messageId) {
        return messageService.findById(messageId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
