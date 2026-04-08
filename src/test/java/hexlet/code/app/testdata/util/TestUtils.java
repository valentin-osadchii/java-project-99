package hexlet.code.app.testdata.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import hexlet.code.app.testdata.model.Task;
import hexlet.code.app.testdata.model.TaskStatus;
import hexlet.code.app.testdata.model.User;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.List;

import hexlet.code.app.testdata.model.Label;

public class TestUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static void saveLabel(MockMvc mockMvc, Label label) throws Exception {
        mockMvc.perform(post("/api/labels").with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(label)))
                .andReturn();
    }

    public static void saveTaskStatus(MockMvc mockMvc, TaskStatus taskStatus) throws Exception {
        mockMvc.perform(post("/api/task_statuses").with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(taskStatus)))
                .andReturn();
    }

    public static void saveUser(MockMvc mockMvc, User user) throws Exception {
        mockMvc.perform(post("/api/users").with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(user)))
                .andReturn();
    }

    public static void saveTask(MockMvc mockMvc, Task task) throws Exception {
        mockMvc.perform(post("/api/tasks").with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(task)))
                .andReturn();
    }

    public static User getUserByEmail(MockMvc mockMvc, String email) throws Exception {
        var response = mockMvc.perform(get("/api/users").with(jwt()))
                .andReturn()
                .getResponse();
        var body = response.getContentAsString();
        var users = MAPPER.readValue(body, new TypeReference<List<User>>() { });

        return users.stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst()
                .orElse(null);
    }

    public static Label getLabelByName(MockMvc mockMvc, String name) throws Exception {
        var response = mockMvc.perform(get("/api/labels").with(jwt()))
                .andReturn()
                .getResponse();
        var body = response.getContentAsString();
        var labels = MAPPER.readValue(body, new TypeReference<List<Label>>() { });

        return labels.stream()
                .filter(label -> label.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static TaskStatus getStatusByName(MockMvc mockMvc, String name) throws Exception {
        var response = mockMvc.perform(get("/api/task_statuses").with(jwt()))
                .andReturn()
                .getResponse();
        var body = response.getContentAsString();
        var statuses = MAPPER.readValue(body, new TypeReference<List<TaskStatus>>() { });

        return statuses.stream()
                .filter(status -> status.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static Task getTaskByName(MockMvc mockMvc, String name) throws Exception {
        var response = mockMvc.perform(get("/api/tasks").with(jwt()))
                .andReturn()
                .getResponse();
        var body = response.getContentAsString();
        var tasks = MAPPER.readValue(body, new TypeReference<List<Task>>() { });

        return tasks.stream()
                .filter(task -> task.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
