package com.twokey.web;

import com.twokey.agent.AgentReply;
import com.twokey.agent.RefundAgent;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer chat endpoint.
 *
 * <p>The customer ID comes from a header to keep the demo simple. In production, take it from the
 * authenticated user (for example the subject of a validated JWT), never from the AI or the message.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final RefundAgent agent;

    public AgentController(RefundAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/chat")
    public AgentReply chat(@RequestHeader("X-Customer-Id") String customerId,
                           @Valid @RequestBody ChatRequest request) {
        return agent.handle(customerId.trim().toUpperCase(), request.message());
    }
}
