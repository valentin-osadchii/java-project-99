package hexlet.code.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
public class Task implements BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Size(min = 1)
    private String name;

    private Integer index;

    private String description;

    @ManyToOne
    @JoinColumn(name = "task_status_id",
            foreignKey = @ForeignKey(foreignKeyDefinition =
                    "FOREIGN KEY (task_status_id) REFERENCES task_statuses(id) ON DELETE RESTRICT"))
    @NotNull
    private TaskStatus taskStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id",
            foreignKey = @ForeignKey(foreignKeyDefinition =
                    "FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE RESTRICT"))
    private User assignee;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDate createdAt;
}
