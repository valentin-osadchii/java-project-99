// src/main/java/hexlet/code/app/config/DataInitializer.java
package hexlet.code.app.config;

import hexlet.code.app.model.User;
import hexlet.code.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
//@Profile("dev")
public class DataInitializer {

    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        if (userRepository.findByEmail("hexlet@example.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("hexlet@example.com");
            admin.setFirstName("Admin");
            admin.setLastName("System");
            admin.setPassword("qwerty");

            userRepository.save(admin);
            System.out.println("✅ Admin user created: hexlet@example.com");
        }
    }
}
