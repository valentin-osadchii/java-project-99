package hexlet.code.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hexlet.code.app.dto.UserCreateDTO;
import hexlet.code.app.dto.UserUpdateDTO;
import hexlet.code.app.model.User;
import hexlet.code.app.repository.UserRepository;
import hexlet.code.app.util.JWTUtils;
import org.junit.jupiter.api.AfterEach;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("UsersController Integration Tests")
class UsersControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTUtils jwtUtils;

    private MockMvc mockMvc;
    private User savedUser;
    private String authToken;

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        userRepository.deleteAll();
        savedUser = createUserAndSave("john@example.com", "John", "Doe", "password123");
        authToken = jwtUtils.generateToken("john@example.com");
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/users - should return empty list when no users exist")
    void getAllUsersWhenNoUsersShouldReturnEmptyList() throws Exception {
        userRepository.deleteAll();

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/users - should return list of all users")
    void getAllUsersWhenUsersExistShouldReturnUserList() throws Exception {
        createUserAndSave("jane@example.com", "Jane", "Smith", "password456");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email", is("john@example.com")))
                .andExpect(jsonPath("$[0].firstName", is("John")))
                .andExpect(jsonPath("$[0].lastName", is("Doe")))
                .andExpect(jsonPath("$[0].createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/users/{id} - should return user by id")
    void getUserByIdWhenUserExistsShouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/" + savedUser.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(savedUser.getId().intValue())))
                .andExpect(jsonPath("$.email", is("john@example.com")))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/users/{id} - should return 404 when user not found")
    void getUserByIdWhenUserNotFoundShouldReturn404() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(get("/api/users/" + nonExistentId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/users - should create new user")
    void createUserWithValidDataShouldCreateUser() throws Exception {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("new@example.com");
        createDTO.setFirstName("New");
        createDTO.setLastName("User");
        createDTO.setPassword("newpassword123");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is("new@example.com")))
                .andExpect(jsonPath("$.firstName", is("New")))
                .andExpect(jsonPath("$.lastName", is("User")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        assertThat(userRepository.findByEmail("new@example.com")).isPresent();
    }

    @Test
    @DisplayName("POST /api/users - should not expose password in response")
    void createUserShouldNotReturnPassword() throws Exception {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("secure@example.com");
        createDTO.setFirstName("Secure");
        createDTO.setLastName("User");
        createDTO.setPassword("secretpassword");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - should update user email")
    void updateUserWhenUpdatingEmailShouldUpdateSuccessfully() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setEmail("updated@example.com");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/users/" + savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("updated@example.com")))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")));

        User updatedUser = userRepository.findById(savedUser.getId()).get();
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    @DisplayName("PUT /api/users/{id} - should update user password")
    void updateUserWhenUpdatingPasswordShouldEncodeAndSave() throws Exception {
        String newPassword = "newSecurePassword123";
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setPassword(newPassword);

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/users/" + savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(savedUser.getId()).get();
        assertThat(updatedUser.getPassword()).isNotEqualTo(newPassword);
        assertThat(updatedUser.getPassword()).startsWith("$2");
    }

    @Test
    @DisplayName("PUT /api/users/{id} - should update multiple fields")
    void updateUserWhenUpdatingMultipleFieldsShouldUpdateAll() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setEmail("newemail@example.com");
        updateDTO.setFirstName("Updated");
        updateDTO.setLastName("Name");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/users/" + savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("newemail@example.com")))
                .andExpect(jsonPath("$.firstName", is("Updated")))
                .andExpect(jsonPath("$.lastName", is("Name")));
    }

    @Test
    @DisplayName("PUT /api/users/{id} - should keep unchanged fields intact")
    void updateUserWhenPartialUpdateShouldKeepUnchangedFields() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setEmail("changed@example.com");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/users/" + savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("changed@example.com")))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")));
    }

    @Test
    @DisplayName("PUT /api/users/{id} - should return 404 when user not found")
    void updateUserWhenUserNotFoundShouldReturn404() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setEmail("test@example.com");

        String requestBody = objectMapper.writeValueAsString(updateDTO);
        Long nonExistentId = 999L;

        mockMvc.perform(put("/api/users/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - should delete user")
    void deleteUserWhenUserExistsShouldDeleteSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/users/" + savedUser.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - should return 404 when user not found")
    void deleteUserWhenUserNotFoundShouldReturn404() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(delete("/api/users/" + nonExistentId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - should not affect other users")
    void deleteUserWhenMultipleUsersShouldDeleteOnlyTarget() throws Exception {
        User anotherUser = createUserAndSave("another@example.com", "Another", "User", "password789");

        mockMvc.perform(delete("/api/users/" + savedUser.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
        assertThat(userRepository.findById(anotherUser.getId())).isPresent();
    }

    @Test
    @DisplayName("POST /api/users - should persist createdAt automatically")
    void createUserShouldAutoGenerateCreatedAt() throws Exception {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("autodate@example.com");
        createDTO.setFirstName("Auto");
        createDTO.setLastName("Date");
        createDTO.setPassword("password123");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        User createdUser = userRepository.findByEmail("autodate@example.com").get();
        assertThat(createdUser.getCreatedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - should update updatedAt automatically")
    void updateUserShouldAutoGenerateUpdatedAt() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setEmail("updated@example.com");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/users/" + savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(savedUser.getId()).get();
        assertThat(updatedUser.getUpdatedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("POST /api/users - should return 400 when email is missing")
    void createUserWhenEmailMissingShouldReturnBadRequest() throws Exception {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail(null);
        createDTO.setFirstName("Test");
        createDTO.setLastName("User");
        createDTO.setPassword("password123");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/users - should return 400 when password is too short")
    void createUserWhenPasswordTooShortShouldReturnBadRequest() throws Exception {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("test@example.com");
        createDTO.setFirstName("Test");
        createDTO.setLastName("User");
        createDTO.setPassword("ab");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/users - should return 400 when request body is empty")
    void createUserWhenEmptyBodyShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - should return 400 when email format is invalid")
    void updateUserWhenInvalidEmailShouldReturnBadRequest() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setEmail("invalid-email");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/users/" + savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - should return 400 when password is too short")
    void updateUserWhenPasswordTooShortShouldReturnBadRequest() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setPassword("ab");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/users/" + savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    private User createUserAndSave(String email, String firstName, String lastName, String password) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }
}
