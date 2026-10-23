package com.twokey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The Two-Key AI Agent.
 *
 * <p>Key 1: an AI agent understands the customer and decides WHAT to do.
 * <br>Key 2: a plain-Java Policy Gate decides IF the action is allowed.
 * <br>Big or unusual cases need an extra human key.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TwoKeyAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TwoKeyAgentApplication.class, args);
    }
}
