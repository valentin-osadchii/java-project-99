package hexlet.code.app.repository;

import hexlet.code.app.model.TaskStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskStatusRepository extends JpaRepository<TaskStatus, Long> {
    List<TaskStatus> findAllBySlugIn(List<String> slugs);

    Optional<TaskStatus> findTaskStatusBySlug(String slug);
}
