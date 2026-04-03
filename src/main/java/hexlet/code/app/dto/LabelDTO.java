package hexlet.code.app.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class LabelDTO {
    private Long id;
    private String name;
    private LocalDate createdAt;
}
