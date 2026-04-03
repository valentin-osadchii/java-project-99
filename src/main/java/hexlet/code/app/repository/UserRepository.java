package hexlet.code.app.repository;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hexlet.code.app.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findById(JsonNullable<Long> assigneeId);
}
