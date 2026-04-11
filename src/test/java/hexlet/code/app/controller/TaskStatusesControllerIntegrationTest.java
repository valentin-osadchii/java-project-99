package hexlet.code.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hexlet.code.app.dto.TaskStatusCreateDTO;
import hexlet.code.app.dto.TaskStatusUpdateDTO;
import hexlet.code.app.model.TaskStatus;
import hexlet.code.app.repository.TaskStatusRepository;
import hexlet.code.app.util.JWTUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "hexlet.data-initializer.enabled=false"
)
@DisplayName("TaskStatusesController Integration Tests")
class TaskStatusesControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private JWTUtils jwtUtils;

    private MockMvc mockMvc;
    private TaskStatus savedStatus;
    private String authToken;

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        taskStatusRepository.deleteAll();
        savedStatus = createStatusAndSave("Test Draft", "test-draft-" + System.currentTimeMillis());
        authToken = jwtUtils.generateToken("test@example.com");
    }

    @AfterEach
    void tearDown() {
        taskStatusRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/task_statuses - should return empty list when no statuses exist")
    void getAllTaskStatusesWhenNoStatusesShouldReturnEmptyList() throws Exception {
        taskStatusRepository.deleteAll();

        mockMvc.perform(get("/api/task_statuses")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/task_statuses - should return list of all task statuses")
    void getAllTaskStatusesWhenStatusesExistShouldReturnStatusList() throws Exception {
        createStatusAndSave("ToReview", "to-review-" + System.currentTimeMillis());
        createStatusAndSave("Done", "done-" + System.currentTimeMillis());

        mockMvc.perform(get("/api/task_statuses")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("Test Draft")))
                .andExpect(jsonPath("$[0].slug", is(savedStatus.getSlug())))
                .andExpect(jsonPath("$[0].createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/task_statuses/{id} - should return task status by id")
    void getTaskStatusByIdWhenStatusExistsShouldReturnStatus() throws Exception {
        mockMvc.perform(get("/api/task_statuses/" + savedStatus.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(savedStatus.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Test Draft")))
                .andExpect(jsonPath("$.slug", is(savedStatus.getSlug())))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/task_statuses/{id} - should return 404 when status not found")
    void getTaskStatusByIdWhenStatusNotFoundShouldReturn404() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(get("/api/task_statuses/" + nonExistentId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/task_statuses - should create new task status")
    void createTaskStatusWithValidDataShouldCreateStatus() throws Exception {
        TaskStatusCreateDTO createDTO = new TaskStatusCreateDTO();
        createDTO.setName("In Progress");
        createDTO.setSlug("in_progress");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/task_statuses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("In Progress")))
                .andExpect(jsonPath("$.slug", is("in_progress")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        assertThat(taskStatusRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("POST /api/task_statuses - should persist createdAt automatically")
    void createTaskStatusShouldAutoGenerateCreatedAt() throws Exception {
        TaskStatusCreateDTO createDTO = new TaskStatusCreateDTO();
        createDTO.setName("Testing");
        createDTO.setSlug("testing");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/task_statuses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        TaskStatus createdStatus = taskStatusRepository.findAll().stream()
                .filter(s -> "Testing".equals(s.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(createdStatus.getCreatedAt()).isEqualTo(LocalDate.now());
    }

    @ParameterizedTest(name = "POST /api/task_statuses - should return 400 when name='' {0} '' and slug='' {1} ''")
    @CsvSource({
        "null, 'valid-slug'",
        "'valid-name', null",
        "'', 'valid-slug'",
        "'valid-name', ''"
    })
    @DisplayName("POST /api/task_statuses - should return 400 when name or slug is missing/empty")
    void createTaskStatusWhenNameOrSlugInvalidShouldReturnBadRequest(String name, String slug) throws Exception {
        TaskStatusCreateDTO createDTO = new TaskStatusCreateDTO();
        createDTO.setName("null".equals(name) ? null : name);
        createDTO.setSlug("null".equals(slug) ? null : slug);

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/task_statuses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/task_statuses - should return 400 when request body is empty")
    void createTaskStatusWhenEmptyBodyShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/task_statuses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/task_statuses/{id} - should update task status name")
    void updateTaskStatusWhenUpdatingNameShouldUpdateSuccessfully() throws Exception {
        TaskStatusUpdateDTO updateDTO = new TaskStatusUpdateDTO();
        updateDTO.setName("Updated Draft");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/task_statuses/" + savedStatus.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Draft")))
                .andExpect(jsonPath("$.slug", is(savedStatus.getSlug())));

        TaskStatus updatedStatus = taskStatusRepository.findById(savedStatus.getId()).get();
        assertThat(updatedStatus.getName()).isEqualTo("Updated Draft");
    }

    @Test
    @DisplayName("PUT /api/task_statuses/{id} - should keep slug unchanged when not provided")
    void updateTaskStatusWhenPartialUpdateShouldKeepSlugUnchanged() throws Exception {
        TaskStatusUpdateDTO updateDTO = new TaskStatusUpdateDTO();
        updateDTO.setName("New Name");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/task_statuses/" + savedStatus.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")))
                .andExpect(jsonPath("$.slug", is(savedStatus.getSlug())))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("PUT /api/task_statuses/{id} - should return 404 when status not found")
    void updateTaskStatusWhenStatusNotFoundShouldReturn404() throws Exception {
        TaskStatusUpdateDTO updateDTO = new TaskStatusUpdateDTO();
        updateDTO.setName("New Name");

        String requestBody = objectMapper.writeValueAsString(updateDTO);
        Long nonExistentId = 999L;

        mockMvc.perform(put("/api/task_statuses/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/task_statuses/{id} - should return 400 when name is empty")
    void updateTaskStatusWhenNameEmptyShouldReturnBadRequest() throws Exception {
        TaskStatusUpdateDTO updateDTO = new TaskStatusUpdateDTO();
        updateDTO.setName("");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/task_statuses/" + savedStatus.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/task_statuses/{id} - should delete task status")
    void deleteTaskStatusWhenStatusExistsShouldDeleteSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/task_statuses/" + savedStatus.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        assertThat(taskStatusRepository.findById(savedStatus.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/task_statuses/{id} - should return 404 when status not found")
    void deleteTaskStatusWhenStatusNotFoundShouldReturn404() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(delete("/api/task_statuses/" + nonExistentId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/task_statuses/{id} - should not affect other statuses")
    void deleteTaskStatusWhenMultipleStatusesShouldDeleteOnlyTarget() throws Exception {
        TaskStatus anotherStatus = createStatusAndSave("Done", "done-" + System.currentTimeMillis());

        mockMvc.perform(delete("/api/task_statuses/" + savedStatus.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        assertThat(taskStatusRepository.findById(savedStatus.getId())).isEmpty();
        assertThat(taskStatusRepository.findById(anotherStatus.getId())).isPresent();
    }

    @Test
    @DisplayName("GET /api/task_statuses - should return X-Total-Count header")
    void getAllTaskStatusesShouldReturnTotalCountHeader() throws Exception {
        createStatusAndSave("ToReview", "to-review-" + System.currentTimeMillis());

        mockMvc.perform(get("/api/task_statuses")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String totalCount = result.getResponse().getHeader("X-Total-Count");
                    assertThat(totalCount).isEqualTo("2");
                });
    }

    private TaskStatus createStatusAndSave(String name, String slug) {
        TaskStatus status = new TaskStatus();
        status.setName(name);
        status.setSlug(slug);
        return taskStatusRepository.save(status);
    }
}
