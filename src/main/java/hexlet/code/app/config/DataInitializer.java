// src/main/java/hexlet/code/app/config/DataInitializer.java
package hexlet.code.app.config;

import hexlet.code.app.model.Label;
import hexlet.code.app.model.TaskStatus;
import hexlet.code.app.model.User;
import hexlet.code.app.repository.LabelRepository;
import hexlet.code.app.repository.TaskStatusRepository;
import hexlet.code.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final LabelRepository labelRepository;

    @Value("${hexlet.data-initializer.enabled:true}")
    private boolean enabled;

    @Value("${hexlet.data-initializer.admin-password:qwerty}")
    private String adminPassword;

    @PostConstruct
    public void init() {
        if (!enabled) {
            return;
        }
        if (userRepository.findByEmail("hexlet@example.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("hexlet@example.com");
            admin.setFirstName("Admin");
            admin.setLastName("System");
            admin.setPassword(passwordEncoder.encode(adminPassword));

            userRepository.save(admin);
            System.out.println("✅ Admin user created: hexlet@example.com");

            initTaskStatuses();
            initLabels();
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
                .map(TaskStatus::getSlug)
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

    @Transactional
    public void initLabels() {
        List<String> defaultLabels = List.of("feature", "bug");

        Set<String> existingNames = labelRepository.findAll().stream()
                .map(Label::getName)
                .collect(Collectors.toSet());

        List<Label> toSave = defaultLabels.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> {
                    Label label = new Label();
                    label.setName(name);
                    return label;
                })
                .toList();

        if (!toSave.isEmpty()) {
            labelRepository.saveAll(toSave);
            toSave.forEach(l -> System.out.println("✅ Label created: " + l.getName()));
        } else {
            System.out.println("ℹ️ All default labels are already up to date.");
        }
    }

    private record StatusSeed(String slug, String name) {
    }

}
