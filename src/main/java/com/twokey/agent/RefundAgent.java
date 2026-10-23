package com.twokey.agent;

/** Key 1: understands the customer and decides WHAT to do. Two implementations: LLM and offline demo. */
public interface RefundAgent {

    AgentReply handle(String customerId, String message);
}
