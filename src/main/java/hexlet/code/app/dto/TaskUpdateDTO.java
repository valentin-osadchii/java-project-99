package hexlet.code.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.openapitools.jackson.nullable.JsonNullable;


public class TaskUpdateDTO {

    @Min(value = 0, message = "Index cannot be negative")
    private JsonNullable<Integer> index;

    private JsonNullable<Long> assigneeId;

    @Size(min = 1)
    private String title;

    private JsonNullable<String> content;

    @NotBlank
    private String status;
}
