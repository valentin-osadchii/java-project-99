package hexlet.code.app.controller;

import tools.jackson.databind.ObjectMapper;
import hexlet.code.app.dto.TaskCreateDTO;
import hexlet.code.app.dto.TaskUpdateDTO;
import hexlet.code.app.model.Label;
import hexlet.code.app.model.Task;
import hexlet.code.app.model.TaskStatus;
import hexlet.code.app.model.User;
import hexlet.code.app.repository.LabelRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private LabelRepository labelRepository;

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
        labelRepository.deleteAll();
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

    // ==================== create() method tests ====================

    @Test
    @DisplayName("POST /api/tasks - should create task with valid data and explicit status")
    void createTaskWithValidDataAndExplicitStatusShouldCreateTask() throws Exception {
        User assignee = createUserAndSave("assignee@example.com", "Assignee", "User", "password123");

        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTitle("New Task");
        createDTO.setContent("Task description");
        createDTO.setIndex(1);
        createDTO.setAssigneeId(assignee.getId());
        createDTO.setStatus("test-status");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        var result = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("New Task")))
                .andExpect(jsonPath("$.content", is("Task description")))
                .andExpect(jsonPath("$.index", is(1)))
                .andExpect(jsonPath("$.status", is("test-status")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Long createdTaskId = objectMapper.readTree(responseBody).get("id").asLong();
        assertThat(taskRepository.findById(createdTaskId)).isPresent();
    }

    @Test
    @DisplayName("POST /api/tasks - should create task with all fields populated")
    void createTaskWithAllFieldsPopulatedShouldCreateTask() throws Exception {
        User assignee = createUserAndSave("dev@example.com", "Dev", "User", "password123");

        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTitle("Complete Feature");
        createDTO.setContent("Implement the new feature");
        createDTO.setIndex(5);
        createDTO.setAssigneeId(assignee.getId());
        createDTO.setStatus("test-status");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        var result = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Complete Feature")))
                .andExpect(jsonPath("$.content", is("Implement the new feature")))
                .andExpect(jsonPath("$.index", is(5)))
                .andExpect(jsonPath("$.status", is("test-status")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Long createdTaskId = objectMapper.readTree(responseBody).get("id").asLong();
        assertThat(taskRepository.findById(createdTaskId)).isPresent();
    }

    @Test
    @DisplayName("POST /api/tasks - should return 404 when status slug does not exist")
    void createTaskWithNonExistentStatusShouldReturn404() throws Exception {
        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTitle("Task with Invalid Status");
        createDTO.setContent("This status doesn't exist");
        createDTO.setStatus("non-existent-slug");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/tasks - should create task with label_ids")
    void createTaskWithLabelIdsShouldSaveAndReturnLabels() throws Exception {
        Label bugLabel = createLabelAndSave("bug");
        Label featureLabel = createLabelAndSave("feature");

        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTitle("Task with labels");
        createDTO.setContent("Description");
        createDTO.setStatus("test-status");
        createDTO.setLabelIds(java.util.List.of(bugLabel.getId(), featureLabel.getId()));

        String requestBody = objectMapper.writeValueAsString(createDTO);

        var result = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Task with labels")))
                .andExpect(jsonPath("$.label_ids", notNullValue()))
                .andExpect(jsonPath("$.label_ids", hasSize(2)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Long createdTaskId = objectMapper.readTree(responseBody).get("id").asLong();
        Task createdTask = taskRepository.findById(createdTaskId).orElseThrow();
        assertThat(createdTask.getLabels()).hasSize(2);
    }

    @Test
    @DisplayName("POST /api/tasks - should create task with empty label_ids")
    void createTaskWithEmptyLabelIdsShouldHaveNoLabels() throws Exception {
        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTitle("Task without labels");
        createDTO.setContent("Description");
        createDTO.setStatus("test-status");
        createDTO.setLabelIds(java.util.List.of());

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label_ids", notNullValue()))
                .andExpect(jsonPath("$.label_ids", hasSize(0)));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should update task label_ids")
    void updateTaskWithLabelIdsShouldUpdateLabels() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Draft", "draft-labels");
        User user = createUserAndSave("labels@example.com", "Labels", "User", "password");
        Label bugLabel = createLabelAndSave("bug");
        Label featureLabel = createLabelAndSave("feature");

        Task task = createTaskAndSave("Task to update", 1, "Description", status, user);
        task.setLabels(new java.util.HashSet<>(java.util.Set.of(bugLabel)));
        taskRepository.save(task);

        TaskUpdateDTO updateDTO = new TaskUpdateDTO();
        updateDTO.setLabelIds(java.util.List.of(featureLabel.getId()));

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label_ids", hasSize(1)))
                .andExpect(jsonPath("$.label_ids[0]", is(featureLabel.getId().intValue())));

        Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(updatedTask.getLabels()).hasSize(1);
        assertThat(updatedTask.getLabels().iterator().next().getId()).isEqualTo(featureLabel.getId());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should remove all labels when label_ids is empty")
    void updateTaskWithEmptyLabelIdsShouldRemoveAllLabels() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Draft", "draft-rm-labels");
        User user = createUserAndSave("rmlabels@example.com", "Rm", "User", "password");
        Label bugLabel = createLabelAndSave("bug");

        Task task = createTaskAndSave("Task to clear labels", 1, "Description", status, user);
        task.setLabels(new java.util.HashSet<>(java.util.Set.of(bugLabel)));
        taskRepository.save(task);

        TaskUpdateDTO updateDTO = new TaskUpdateDTO();
        updateDTO.setLabelIds(java.util.List.of());

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label_ids", hasSize(0)));

        Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(updatedTask.getLabels()).isEmpty();
    }

    @Test
    @DisplayName("POST /api/tasks - should return 400 when status is missing")
    void createTaskWithMissingStatusShouldReturn400() throws Exception {
        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTitle("Task Without Status");
        createDTO.setContent("Missing status field");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks - should return 400 when status is empty string")
    void createTaskWithEmptyStatusShouldReturn400() throws Exception {
        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTitle("Task with Empty Status");
        createDTO.setContent("Empty status string");
        createDTO.setStatus("");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks - should return 400 when title is empty")
    void createTaskWithEmptyTitleShouldReturn400() throws Exception {
        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTitle("");
        createDTO.setContent("Task with empty title");
        createDTO.setStatus("test-status");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks - should return 400 when index is negative")
    void createTaskWithNegativeIndexShouldReturn400() throws Exception {
        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTitle("Task with Negative Index");
        createDTO.setContent("Invalid index");
        createDTO.setIndex(-1);
        createDTO.setStatus("test-status");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks - should return 400 when request body is empty")
    void createTaskWithEmptyBodyShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks - should return 401 when not authenticated")
    void createTaskWithoutAuthShouldReturn401() throws Exception {
        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTitle("Unauthorized Task");
        createDTO.setContent("Should not be created");
        createDTO.setStatus("test-status");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    // ==================== update() method tests ====================

    @Test
    @DisplayName("PUT /api/tasks/{id} - should update task title")
    void updateTaskTitleShouldUpdateSuccessfully() throws Exception {
        String requestBody = """
                {
                  "title": "Updated Task Title"
                }
                """;

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Task Title")))
                .andExpect(jsonPath("$.index", is(1)))
                .andExpect(jsonPath("$.content", is("Test Description")));

        Task updatedTask = taskRepository.findById(savedTask.getId()).get();
        assertThat(updatedTask.getName()).isEqualTo("Updated Task Title");
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should update task status")
    void updateTaskStatusShouldUpdateSuccessfully() throws Exception {
        TaskStatus newStatus = createTaskStatusAndSave("In Progress", "in-progress");

        String requestBody = """
                {
                  "status": "in-progress"
                }
                """;

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("in-progress")));

        Task updatedTask = taskRepository.findById(savedTask.getId()).get();
        assertThat(updatedTask.getTaskStatus().getSlug()).isEqualTo("in-progress");
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should update task assignee")
    void updateTaskAssigneeShouldUpdateSuccessfully() throws Exception {
        User newAssignee = createUserAndSave("new@example.com", "New", "User", "password123");

        String requestBody = """
                {
                  "assigneeId": %d
                }
                """.formatted(newAssignee.getId());

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee_id", is(newAssignee.getId().intValue())));

        Task updatedTask = taskRepository.findById(savedTask.getId()).get();
        assertThat(updatedTask.getAssignee().getId()).isEqualTo(newAssignee.getId());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should update multiple fields")
    void updateTaskMultipleFieldsShouldUpdateAll() throws Exception {
        TaskStatus newStatus = createTaskStatusAndSave("Done", "done");
        User newAssignee = createUserAndSave("assignee2@example.com", "Assignee", "Two", "password123");

        String requestBody = """
                {
                  "title": "Completely Updated Task",
                  "content": "New content here",
                  "index": 10,
                  "status": "done",
                  "assigneeId": %d
                }
                """.formatted(newAssignee.getId());

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Completely Updated Task")))
                .andExpect(jsonPath("$.content", is("New content here")))
                .andExpect(jsonPath("$.index", is(10)))
                .andExpect(jsonPath("$.status", is("done")))
                .andExpect(jsonPath("$.assignee_id", is(newAssignee.getId().intValue())));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should keep unchanged fields intact")
    void updateTaskPartialUpdateShouldKeepUnchangedFields() throws Exception {
        String requestBody = """
                {
                  "title": "Only Title Changed"
                }
                """;

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Only Title Changed")))
                .andExpect(jsonPath("$.index", is(1)))
                .andExpect(jsonPath("$.content", is("Test Description")))
                .andExpect(jsonPath("$.status", is("test-status")));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should return 404 when task not found")
    void updateTaskWhenTaskNotFoundShouldReturn404() throws Exception {
        Long nonExistentId = 999L;

        String requestBody = """
                {
                  "title": "Won't be created"
                }
                """;

        mockMvc.perform(put("/api/tasks/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should return 404 when status not found")
    void updateTaskWithNonExistentStatusShouldReturn404() throws Exception {
        String requestBody = """
                {
                  "status": "non-existent-slug"
                }
                """;

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should return 404 when assignee not found")
    void updateTaskWithNonExistentAssigneeShouldReturn404() throws Exception {
        String requestBody = """
                {
                  "assigneeId": 999
                }
                """;

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should return 400 when title is empty")
    void updateTaskWithEmptyTitleShouldReturn400() throws Exception {
        String requestBody = """
                {
                  "title": ""
                }
                """;

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should return 400 when index is negative")
    void updateTaskWithNegativeIndexShouldReturn400() throws Exception {
        String requestBody = """
                {
                  "index": -5
                }
                """;

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should return 401 when not authenticated")
    void updateTaskWithoutAuthShouldReturn401() throws Exception {
        String requestBody = """
                {
                  "title": "Unauthorized Update"
                }
                """;

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
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

    // ========== Pagination Tests ==========

    @Test
    @DisplayName("GET /api/tasks - should respect _perPage parameter")
    void getAllTasksWithPerPageShouldLimitResults() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Draft", "draft-pagination");
        User user = createUserAndSave("page@example.com", "Page", "User", "password");
        for (int i = 1; i <= 15; i++) {
            createTaskAndSave("Task " + i, i, "Description " + i, status, user);
        }

        mockMvc.perform(get("/api/tasks")
                        .param("_page", "1")
                        .param("_perPage", "10")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));
    }

    @Test
    @DisplayName("GET /api/tasks - should return second page with _page=2")
    void getAllTasksWithPageTwoShouldReturnNextBatch() throws Exception {
        taskRepository.deleteAll();
        TaskStatus status = createTaskStatusAndSave("Draft", "draft-page2");
        User user = createUserAndSave("page2@example.com", "Page2", "User", "password");
        for (int i = 1; i <= 15; i++) {
            createTaskAndSave("Task " + i, i, "Description " + i, status, user);
        }

        mockMvc.perform(get("/api/tasks")
                        .param("offset", "2")
                        .param("limit", "10")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    // ========== Filter Tests ==========

    @Test
    @DisplayName("GET /api/tasks?titleCont= - should filter tasks by title substring")
    void getTasksFilteredByTitleShouldReturnMatchingTasks() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Draft", "draft-title");
        User user = createUserAndSave("title@example.com", "Title", "User", "password");
        createTaskAndSave("Bug fix", 1, "Description", status, user);
        createTaskAndSave("Feature implementation", 2, "Description", status, user);
        createTaskAndSave("Bug in login", 3, "Description", status, user);

        mockMvc.perform(get("/api/tasks")
                        .param("titleCont", "Bug")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Bug fix")))
                .andExpect(jsonPath("$[1].title", is("Bug in login")));
    }

    @Test
    @DisplayName("GET /api/tasks?assigneeId= - should filter tasks by assignee")
    void getTasksFilteredByAssigneeShouldReturnMatchingTasks() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Draft", "draft-assignee");
        User user1 = createUserAndSave("user1@example.com", "User", "One", "password");
        User user2 = createUserAndSave("user2@example.com", "User", "Two", "password");
        createTaskAndSave("Task by User1", 1, "Description", status, user1);
        createTaskAndSave("Another by User1", 2, "Description", status, user1);
        createTaskAndSave("Task by User2", 3, "Description", status, user2);

        mockMvc.perform(get("/api/tasks")
                        .param("assigneeId", String.valueOf(user1.getId()))
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/tasks?status= - should filter tasks by status slug")
    void getTasksFilteredByStatusShouldReturnMatchingTasks() throws Exception {
        TaskStatus draft = createTaskStatusAndSave("Draft", "draft-filter");
        TaskStatus review = createTaskStatusAndSave("Review", "to_review-filter");
        User user = createUserAndSave("status@example.com", "Status", "User", "password");
        createTaskAndSave("Draft task", 1, "Description", draft, user);
        createTaskAndSave("Review task", 2, "Description", review, user);

        mockMvc.perform(get("/api/tasks")
                        .param("status", "to_review-filter")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Review task")));
    }

    @Test
    @DisplayName("GET /api/tasks?labelId= - should filter tasks by label")
    void getTasksFilteredByLabelShouldReturnMatchingTasks() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Draft", "draft-label");
        User user = createUserAndSave("label@example.com", "Label", "User", "password");
        Label bugLabel = createLabelAndSave("bug");
        Label featureLabel = createLabelAndSave("feature");

        Task task1 = createTaskAndSave("Bug fix", 1, "Fix crash", status, user);
        Task task2 = createTaskAndSave("New feature", 2, "Add feature", status, user);
        Task task3 = createTaskAndSave("Both", 3, "Has both", status, user);

        task1.setLabels(new java.util.HashSet<>(java.util.Set.of(bugLabel)));
        task2.setLabels(new java.util.HashSet<>(java.util.Set.of(featureLabel)));
        task3.setLabels(new java.util.HashSet<>(java.util.Set.of(bugLabel, featureLabel)));
        taskRepository.saveAll(java.util.List.of(task1, task2, task3));

        mockMvc.perform(get("/api/tasks")
                        .param("labelId", String.valueOf(bugLabel.getId()))
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/tasks - should apply multiple filters together")
    void getTasksWithMultipleFiltersShouldReturnIntersectedResults() throws Exception {
        TaskStatus draft = createTaskStatusAndSave("Draft", "draft-multi");
        User user = createUserAndSave("multi@example.com", "Multi", "User", "password");
        Label bugLabel = createLabelAndSave("multi-bug");

        Task task1 = createTaskAndSave("Critical Bug", 1, "Critical issue", draft, user);
        createTaskAndSave("Minor Bug", 2, "Minor issue", draft, user);
        createTaskAndSave("Feature", 3, "New thing", draft, user);

        task1.setLabels(new java.util.HashSet<>(java.util.Set.of(bugLabel)));
        taskRepository.save(task1);

        mockMvc.perform(get("/api/tasks")
                        .param("titleCont", "Bug")
                        .param("labelId", String.valueOf(bugLabel.getId()))
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Critical Bug")));
    }

    @Test
    @DisplayName("GET /api/tasks?titleCont= with no match - should return empty list")
    void getTasksByTitleWithNoMatchShouldReturnEmptyList() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Draft", "draft-nomatch");
        User user = createUserAndSave("nomatch@example.com", "No", "Match", "password");
        createTaskAndSave("Some task", 1, "Description", status, user);

        mockMvc.perform(get("/api/tasks")
                        .param("titleCont", "nonexistent")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/tasks?assigneeId= with non-existent assignee - should return empty list")
    void getTasksByAssigneeWithNonExistentIdShouldReturnEmptyList() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Draft", "draft-noassignee");
        User user = createUserAndSave("noassignee@example.com", "No", "Assignee", "password");
        createTaskAndSave("Some task", 1, "Description", status, user);

        mockMvc.perform(get("/api/tasks")
                        .param("assigneeId", "99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/tasks with no params - should use default pagination")
    void getAllTasksWithDefaultPagination() throws Exception {
        TaskStatus status = createTaskStatusAndSave("Draft", "draft-default");
        User user = createUserAndSave("default@example.com", "Default", "User", "password");
        for (int i = 1; i <= 15; i++) {
            createTaskAndSave("Task " + i, i, "Description", status, user);
        }

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));
    }

    private TaskStatus createTaskStatusAndSave(String name, String slug) {
        TaskStatus status = new TaskStatus();
        status.setName(name);
        status.setSlug(slug);
        return taskStatusRepository.save(status);
    }

    private User createUserAndSave(String email, String firstName, String lastName, String password) {
        User user = new User();
        user.setEmail(email + "-" + System.nanoTime());
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

    private Label createLabelAndSave(String name) {
        Label label = new Label();
        label.setName(name + "-" + System.nanoTime());
        return labelRepository.save(label);
    }
}
