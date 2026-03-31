package hexlet.code.app.controller;

import tools.jackson.databind.ObjectMapper;
import hexlet.code.app.model.Task;
import hexlet.code.app.model.TaskStatus;
import hexlet.code.app.model.User;
import hexlet.code.app.repository.TaskRepository;
import hexlet.code.app.repository.TaskStatusRepository;
import hexlet.code.app.repository.UserRepository;
import hexlet.code.app.util.JWTUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "hexlet.data-initializer.enabled=false"
)
@Transactional
@DisplayName("TaskController Integration Tests")
class TaskControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTUtils jwtUtils;

    private MockMvc mockMvc;
    private Task savedTask;
    private String authToken;

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        taskRepository.deleteAll();
        taskStatusRepository.deleteAll();
        userRepository.deleteAll();

        TaskStatus status = createTaskStatusAndSave("Test Status", "test-status");
        User user = createUserAndSave("test@example.com", "Test", "User", "password123");
        savedTask = createTaskAndSave("Test Task", 1, "Test Description", status, user);

        authToken = jwtUtils.generateToken("test@example.com");
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - should return task by id")
    void getTaskByIdWhenTaskExistsShouldReturnTask() throws Exception {
        mockMvc.perform(get("/api/tasks/" + savedTask.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(savedTask.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Test Task")))
                .andExpect(jsonPath("$.index", is(1)))
                .andExpect(jsonPath("$.content", is("Test Description")))
                .andExpect(jsonPath("$.assignee_id", is(savedTask.getAssignee().getId().intValue())))
                .andExpect(jsonPath("$.status", is("test-status")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - should return 404 when task not found")
    void getTaskByIdWhenTaskNotFoundShouldReturn404() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(get("/api/tasks/" + nonExistentId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - should return task without assignee when assignee is null")
    void getTaskByIdWhenNoAssigneeShouldReturnTaskWithoutAssignee() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Another Status", "another-status");
        Task taskWithoutAssignee = createTaskAndSave("No Assignee Task", 2, "No Assignee", status, null);

        mockMvc.perform(get("/api/tasks/" + taskWithoutAssignee.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(taskWithoutAssignee.getId().intValue())))
                .andExpect(jsonPath("$.title", is("No Assignee Task")))
                .andExpect(jsonPath("$.assignee_id").doesNotExist());
    }

    // ==================== index() method tests ====================

    @Test
    @DisplayName("GET /api/tasks - should return empty list when no tasks exist")
    void getAllTasksWhenNoTasksShouldReturnEmptyList() throws Exception {
        taskRepository.deleteAll();

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(header().string("X-Total-Count", "0"));
    }

    @Test
    @DisplayName("GET /api/tasks - should return list of all tasks")
    void getAllTasksWhenTasksExistShouldReturnTaskList() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Another Status", "another-status");
        User user = createUserAndSave("another@example.com", "Another", "User", "password456");
        createTaskAndSave("Second Task", 2, "Second Description", status, user);

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(savedTask.getId().intValue())))
                .andExpect(jsonPath("$[0].title", is("Test Task")))
                .andExpect(jsonPath("$[0].index", is(1)))
                .andExpect(jsonPath("$[0].content", is("Test Description")))
                .andExpect(jsonPath("$[0].createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/tasks - should return X-Total-Count header with total tasks count")
    void getAllTasksShouldReturnTotalCountHeader() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Another Status", "another-status");
        User user = createUserAndSave("another@example.com", "Another", "User", "password456");
        createTaskAndSave("Second Task", 2, "Second Description", status, user);
        createTaskAndSave("Third Task", 3, "Third Description", status, user);

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"));
    }

    @Test
    @DisplayName("GET /api/tasks - should return 401 when not authenticated")
    void getAllTasksWithoutAuthShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    private TaskStatus createTaskStatusAndSave(String name, String slug) {
        TaskStatus status = new TaskStatus();
        status.setName(name);
        status.setSlug(slug);
        return taskStatusRepository.save(status);
    }

    private User createUserAndSave(String email, String firstName, String lastName, String password) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    private Task createTaskAndSave(String title, Integer index, String content, TaskStatus status, User assignee) {
        Task task = new Task();
        task.setName(title);
        task.setIndex(index);
        task.setDescription(content);
        task.setTaskStatus(status);
        task.setCreatedAt(LocalDate.now());
        if (assignee != null) {
            task.setAssignee(assignee);
        }
        return taskRepository.save(task);
    }
}
