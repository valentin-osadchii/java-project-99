package hexlet.code.app.controller;

import tools.jackson.databind.ObjectMapper;
import hexlet.code.app.dto.LabelCreateDTO;
import hexlet.code.app.dto.LabelUpdateDTO;
import hexlet.code.app.model.Label;
import hexlet.code.app.repository.LabelRepository;
import hexlet.code.app.util.JWTUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
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
@Transactional
@DisplayName("LabelsController Integration Tests")
class LabelsControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private JWTUtils jwtUtils;

    private MockMvc mockMvc;
    private Label savedLabel;
    private String authToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        labelRepository.deleteAll();
        savedLabel = createLabelAndSave("test-label-" + System.currentTimeMillis());
        authToken = jwtUtils.generateToken("test@example.com");
    }

    @Test
    @DisplayName("GET /api/labels - should return empty list when no labels exist")
    void getAllLabelsWhenNoLabelsShouldReturnEmptyList() throws Exception {
        labelRepository.deleteAll();

        mockMvc.perform(get("/api/labels")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/labels - should return list of all labels")
    void getAllLabelsWhenLabelsExistShouldReturnLabelsList() throws Exception {
        createLabelAndSave("another-label");

        mockMvc.perform(get("/api/labels")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is(savedLabel.getName())))
                .andExpect(jsonPath("$[0].createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/labels/{id} - should return label by id")
    void getLabelByIdWhenLabelExistsShouldReturnLabel() throws Exception {
        mockMvc.perform(get("/api/labels/" + savedLabel.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(savedLabel.getId().intValue())))
                .andExpect(jsonPath("$.name", is(savedLabel.getName())))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/labels/{id} - should return 404 when label not found")
    void getLabelByIdWhenLabelNotFoundShouldReturn404() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(get("/api/labels/" + nonExistentId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/labels - should create new label")
    void createLabelWithValidDataShouldCreateLabel() throws Exception {
        LabelCreateDTO createDTO = new LabelCreateDTO();
        createDTO.setName("documentation");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        ResultActions result = mockMvc.perform(post("/api/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("documentation")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        assertThat(labelRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("POST /api/labels - should persist createdAt automatically")
    void createLabelShouldAutoGenerateCreatedAt() throws Exception {
        LabelCreateDTO createDTO = new LabelCreateDTO();
        createDTO.setName("bugfix");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        Label createdLabel = labelRepository.findAll().stream()
                .filter(l -> "bugfix".equals(l.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(createdLabel.getCreatedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("POST /api/labels - should return 400 when name is missing")
    void createLabelWhenNameMissingShouldReturnBadRequest() throws Exception {
        LabelCreateDTO createDTO = new LabelCreateDTO();
        createDTO.setName(null);

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/labels - should return 400 when name is too short (less than 3 chars)")
    void createLabelWhenNameTooShortShouldReturnBadRequest() throws Exception {
        LabelCreateDTO createDTO = new LabelCreateDTO();
        createDTO.setName("ab");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/labels - should return 400 when request body is empty")
    void createLabelWhenEmptyBodyShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/labels/{id} - should update label name")
    void updateLabelWhenUpdatingNameShouldUpdateSuccessfully() throws Exception {
        LabelUpdateDTO updateDTO = new LabelUpdateDTO();
        updateDTO.setName("updated-label");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/labels/" + savedLabel.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("updated-label")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        Label updatedLabel = labelRepository.findById(savedLabel.getId()).get();
        assertThat(updatedLabel.getName()).isEqualTo("updated-label");
    }

    @Test
    @DisplayName("PUT /api/labels/{id} - should return 404 when label not found")
    void updateLabelWhenLabelNotFoundShouldReturn404() throws Exception {
        LabelUpdateDTO updateDTO = new LabelUpdateDTO();
        updateDTO.setName("New Name");

        String requestBody = objectMapper.writeValueAsString(updateDTO);
        Long nonExistentId = 999L;

        mockMvc.perform(put("/api/labels/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/labels/{id} - should return 400 when name is too short")
    void updateLabelWhenNameTooShortShouldReturnBadRequest() throws Exception {
        LabelUpdateDTO updateDTO = new LabelUpdateDTO();
        updateDTO.setName("xy");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/labels/" + savedLabel.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/labels/{id} - should delete label")
    void deleteLabelWhenLabelExistsShouldDeleteSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/labels/" + savedLabel.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        assertThat(labelRepository.findById(savedLabel.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/labels/{id} - should return 404 when label not found")
    void deleteLabelWhenLabelNotFoundShouldReturn404() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(delete("/api/labels/" + nonExistentId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/labels - should return X-Total-Count header")
    void getAllLabelsShouldReturnTotalCountHeader() throws Exception {
        createLabelAndSave("another-label");

        mockMvc.perform(get("/api/labels")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String totalCount = result.getResponse().getHeader("X-Total-Count");
                    assertThat(totalCount).isEqualTo("2");
                });
    }

    @Test
    @DisplayName("POST /api/labels - should return 401 without authentication")
    void createLabelWithoutAuthShouldReturnUnauthorized() throws Exception {
        LabelCreateDTO createDTO = new LabelCreateDTO();
        createDTO.setName("new-label");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/labels - should return 401 without authentication")
    void getAllLabelsWithoutAuthShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/labels"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/labels/{id} - should return 401 without authentication")
    void updateLabelWithoutAuthShouldReturnUnauthorized() throws Exception {
        LabelUpdateDTO updateDTO = new LabelUpdateDTO();
        updateDTO.setName("updated");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/labels/" + savedLabel.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/labels/{id} - should return 401 without authentication")
    void deleteLabelWithoutAuthShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/labels/" + savedLabel.getId()))
                .andExpect(status().isUnauthorized());
    }

    private Label createLabelAndSave(String name) {
        Label label = new Label();
        label.setName(name);
        return labelRepository.save(label);
    }
}
