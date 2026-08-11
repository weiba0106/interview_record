package com.interviewrecord.common.error;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ValidationProbeController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.ValidationProbeController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void returnsStableValidationShape() throws Exception {
        mvc.perform(post("/test/validation")
                        .with(user("probe"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name").value("名称不能为空"));
    }

    @RestController
    public static class ValidationProbeController {

        @PostMapping("/test/validation")
        void validate(@Valid @RequestBody ValidationProbe request) {
        }
    }

    record ValidationProbe(@NotBlank(message = "名称不能为空") String name) {
    }
}
