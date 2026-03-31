package hexlet.code.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

import lombok.Data;

@Data
public class TaskDTO {
    private Long id;
    private Integer index;
    private LocalDate createdAt;

    @JsonProperty("assignee_id")
    private Long assigneeId;

    private String title;
    private String content;
    private String status;
}
