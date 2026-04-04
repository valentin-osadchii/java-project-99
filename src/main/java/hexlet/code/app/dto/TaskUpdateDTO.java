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

    private Long assigneeId;

    @Size(min = 1)
    private String title;

    private String content;

    private String status;

    @JsonProperty("label_ids")
    private List<Long> labelIds;
}
