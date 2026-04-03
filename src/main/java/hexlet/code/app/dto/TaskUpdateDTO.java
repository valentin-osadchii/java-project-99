package hexlet.code.app.dto;

import java.util.List;

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

    private List<Long> labelIds;
}
