// src/main/java/hexlet/code/app/config/DataInitializer.java
package hexlet.code.app.config;

import hexlet.code.app.model.TaskStatus;
import hexlet.code.app.model.User;
import hexlet.code.app.repository.TaskStatusRepository;
import hexlet.code.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaskStatusRepository taskStatusRepository;

    @PostConstruct
    public void init() {
        if (userRepository.findByEmail("hexlet@example.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("hexlet@example.com");
            admin.setFirstName("Admin");
            admin.setLastName("System");
            admin.setPassword(passwordEncoder.encode("qwerty"));

            userRepository.save(admin);
            System.out.println("✅ Admin user created: hexlet@example.com");

            initTaskStatuses();
        }
    }

    @Transactional
    public void initTaskStatuses(String... args) {
        List<StatusSeed> defaults = List.of(
                new StatusSeed("draft", "черновик"),
                new StatusSeed("to_review", "готов к проверке"),
                new StatusSeed("to_be_fixed", "нужно исправить"),
                new StatusSeed("to_publish", "к публикации"),
                new StatusSeed("published", "опубликован")
        );

        List<String> targetSlugs = defaults.stream().map(StatusSeed::slug).toList();
        Set<String> existingSlugs = taskStatusRepository.findAllBySlugIn(targetSlugs)
                .stream()
                .map(TaskStatus::getSlug) // или .slug() если у вас getter без get
                .collect(Collectors.toSet());

        List<TaskStatus> toSave = defaults.stream()
                .filter(seed -> !existingSlugs.contains(seed.slug()))
                .map(seed -> {
                    TaskStatus status = new TaskStatus();
                    status.setSlug(seed.slug());
                    status.setName(seed.name());
                    return status;
                })
                .toList();

        if (!toSave.isEmpty()) {
            taskStatusRepository.saveAll(toSave);
            toSave.forEach(s -> System.out.println("✅ Task status created: " + s.getSlug()));
        } else {
            System.out.println("ℹ️ All task statuses are already up to date.");
        }
    }

    private record StatusSeed(String slug, String name) {}

}
