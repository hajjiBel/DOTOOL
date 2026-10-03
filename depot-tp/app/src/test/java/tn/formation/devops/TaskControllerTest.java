package tn.formation.devops;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void createTaskReturns201() throws Exception {
        mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Configurer Jenkins\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Configurer Jenkins"))
                .andExpect(jsonPath("$.done").value(false));
    }

    @Test
    void blankTitleReturns400() throws Exception {
        mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTaskReturns404() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/tasks/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void simulatedErrorReturns500() throws Exception {
        mvc.perform(get("/api/simulate/error")).andExpect(status().isInternalServerError());
    }

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void homePageIsRendered() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }
}
