package com.twokey.web;

import com.twokey.store.DemoData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End to end over HTTP, in offline mode (no API key needed). */
@SpringBootTest(properties = "twokey.agent.mode=offline")
@AutoConfigureMockMvc
class TwoKeyAgentApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DemoData demoData;

    @BeforeEach
    void resetData() {
        demoData.reset();
    }

    @Test
    void chatRefundsADuplicateCharge() throws Exception {
        mvc.perform(post("/api/agent/chat")
                        .header("X-Customer-Id", "C-100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"I was charged twice for order A-1042\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("offline"))
                .andExpect(jsonPath("$.reply", containsString("$49.99")));

        mvc.perform(get("/api/demo/customers/C-100/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.order.id == 'A-1042')].refunds[0].status").value("COMPLETED"));
    }

    @Test
    void bigRefundWaitsForTheHumanKeyThenCompletes() throws Exception {
        mvc.perform(post("/api/agent/chat")
                        .header("X-Customer-Id", "C-100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"I paid twice for order A-3003\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("specialist")));

        mvc.perform(get("/api/approvals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("R-1001"));

        mvc.perform(post("/api/approvals/R-1001/approve").header("X-Approver", "lead-jane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(get("/api/approvals"))
                .andExpect(jsonPath("$", hasSize(0)));

        // Approving twice is a conflict, not a second refund.
        mvc.perform(post("/api/approvals/R-1001/approve"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsAnEmptyMessage() throws Exception {
        mvc.perform(post("/api/agent/chat")
                        .header("X-Customer-Id", "C-100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownRefundIsNotFound() throws Exception {
        mvc.perform(post("/api/approvals/R-9999/approve"))
                .andExpect(status().isNotFound());
    }
}
