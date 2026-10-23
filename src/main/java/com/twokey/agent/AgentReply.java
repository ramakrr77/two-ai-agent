package com.twokey.agent;

/**
 * What the agent says back to the customer.
 *
 * @param mode "llm" or "offline", so you can see which planner answered
 */
public record AgentReply(String reply, String mode) {
}
