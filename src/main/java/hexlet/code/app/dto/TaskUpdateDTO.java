package hexlet.code.app.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.Data;


@Data
public class TaskUpdateDTO {

    @Min(value = 0, message = "Index cannot be negative")
    private Integer index;

    @JsonProperty("assignee_id")
    private Long assigneeId;

    @Size(min = 1)
    private String title;

    private String content;

    private String status;

    @JsonProperty("label_ids")
    private List<Long> labelIds;

    /**
     * Tracks whether assignee_id was present in the JSON request.
     * - present with value  → set assignee
     * - present with null   → clear assignee
     * - not present         → leave unchanged
     */
    private transient boolean assigneeIdSet = false;

    @JsonProperty("assignee_id")
    public void setAssigneeId(Long id) {
        this.assigneeId = id;
        this.assigneeIdSet = true;
    }

    public boolean isAssigneeIdSet() {
        return assigneeIdSet;
    }
}
