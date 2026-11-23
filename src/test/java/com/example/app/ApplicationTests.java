package com.example.app;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsMemo() throws Exception {
        mockMvc.perform(post("/api/memos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/memos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("hello"));
    }

    @Test
    void rejectsBlankMemo() throws Exception {
        mockMvc.perform(post("/api/memos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void servesJavaScript() throws Exception {
        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/memos")));
    }
}
