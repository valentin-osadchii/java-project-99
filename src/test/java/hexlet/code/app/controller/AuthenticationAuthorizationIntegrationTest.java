package hexlet.code.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hexlet.code.app.dto.AuthRequest;
import hexlet.code.app.dto.UserCreateDTO;
import hexlet.code.app.dto.UserUpdateDTO;
import hexlet.code.app.model.User;
import hexlet.code.app.repository.UserRepository;
import hexlet.code.app.util.JWTUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@DisplayName("Authentication and Authorization Integration Tests")
class AuthenticationAuthorizationIntegrationTest {

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
    private User user1;
    private User user2;
    private String user1Token;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        userRepository.deleteAll();

        // Create two test users and flush to ensure they're committed
        user1 = createUserAndSave("user1@example.com", "User", "One", "password123");
        user2 = createUserAndSave("user2@example.com", "User", "Two", "password456");

        // Flush to ensure data is written to database
        userRepository.flush();

        // Generate JWT tokens for both users using JWTUtils directly
        user1Token = jwtUtils.generateToken("user1@example.com");
    }

    // ==================== Authentication Tests ====================

    @Test
    @DisplayName("POST /api/login - should return JWT token for valid credentials")
    void loginWithValidCredentialsShouldReturnToken() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("user1@example.com");
        authRequest.setPassword("password123");

        String requestBody = objectMapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Verify response is a valid JWT token (non-empty string with 3 parts)
        String response = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).isNotBlank();
        assertThat(response.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("POST /api/login - should return 401 for invalid password")
    void loginWithInvalidPasswordShouldReturnUnauthorized() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("user1@example.com");
        authRequest.setPassword("wrongpassword");

        String requestBody = objectMapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/login - should return 401 for non-existent user")
    void loginWithNonExistentUserShouldReturnUnauthorized() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("nonexistent@example.com");
        authRequest.setPassword("password123");

        String requestBody = objectMapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Authorization Tests ====================

    @Test
    @DisplayName("PUT /api/users/{id} - should allow user to update their own profile")
    void updateUserOwnProfileShouldSucceed() throws Exception {
        // Fetch fresh user from database to ensure ID is correct
        User freshUser1 = userRepository.findByEmail("user1@example.com").orElseThrow();

        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setFirstName("Updated");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        String result = mockMvc.perform(put("/api/users/" + freshUser1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Updated")))
                .andReturn().getResponse().getContentAsString();

        // Verify the response contains the updated data
        assertThat(result).contains("Updated");
    }

    @Test
    @DisplayName("PUT /api/users/{id} - should return 403 when updating another user's profile")
    void updateUserAnotherUserProfileShouldReturnForbidden() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setFirstName("Hacked");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/users/" + user2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());

        // Verify the other user's data was not changed
        User user2After = userRepository.findById(user2.getId()).get();
        assertThat(user2After.getFirstName()).isEqualTo("User");
    }

    @Test
    @DisplayName("PUT /api/users/{id} - should return 401 without authentication")
    void updateUserWithoutAuthShouldReturnUnauthorized() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setFirstName("Hacked");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/users/" + user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - should return 401 with invalid token")
    void updateUserWithInvalidTokenShouldReturnUnauthorized() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setFirstName("Hacked");

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/api/users/" + user1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer invalid_token_here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - should allow user to delete their own account")
    void deleteUserOwnAccountShouldSucceed() throws Exception {
        // Fetch fresh user from database to ensure ID is correct
        User freshUser1 = userRepository.findByEmail("user1@example.com").get();

        mockMvc.perform(delete("/api/users/" + freshUser1.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        // Verify user is deleted
        assertThat(userRepository.findById(freshUser1.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - should return 403 when deleting another user")
    void deleteUserAnotherUserShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/" + user2.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());

        // Verify the other user was not deleted
        assertThat(userRepository.findById(user2.getId())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - should return 401 without authentication")
    void deleteUserWithoutAuthShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/users/" + user1.getId()))
                .andExpect(status().isUnauthorized());

        // Verify user was not deleted
        assertThat(userRepository.findById(user1.getId())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - should return 401 with invalid token")
    void deleteUserWithInvalidTokenShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/users/" + user1.getId())
                        .header("Authorization", "Bearer invalid_token_here"))
                .andExpect(status().isUnauthorized());

        // Verify user was not deleted
        assertThat(userRepository.findById(user1.getId())).isPresent();
    }

    @Test
    @DisplayName("GET /api/users - should return 401 without authentication")
    void getUsersWithoutAuthShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users - should return users list with valid token")
    void getUsersWithValidTokenShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/users/{id} - should return 401 without authentication")
    void getUserWithoutAuthShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/" + user1.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users/{id} - should return user with valid token")
    void getUserWithValidTokenShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/users/" + user1.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("user1@example.com")));
    }

    @Test
    @DisplayName("POST /api/users - should work without authentication (registration)")
    void createUserWithoutAuthShouldSucceed() throws Exception {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("newuser@example.com");
        createDTO.setFirstName("New");
        createDTO.setLastName("User");
        createDTO.setPassword("newpassword123");

        String requestBody = objectMapper.writeValueAsString(createDTO);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("newuser@example.com")));
    }

    @Test
    @DisplayName("POST /api/login - should work without authentication")
    void loginWithoutAuthShouldSucceed() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("user1@example.com");
        authRequest.setPassword("password123");

        String requestBody = objectMapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    // Helper method to create and save a user
    private User createUserAndSave(String email, String firstName, String lastName, String password) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }
}
