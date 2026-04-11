package hexlet.code.app.service;

import hexlet.code.app.dto.TaskCreateDTO;
import hexlet.code.app.dto.TaskDTO;
import hexlet.code.app.dto.TaskUpdateDTO;
import hexlet.code.app.model.Task;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface TaskService {

    List<TaskDTO> getAll(Specification<Task> spec, Pageable pageable);

    TaskDTO getTask(long id);

    TaskDTO create(TaskCreateDTO taskData);

    TaskDTO update(long id, TaskUpdateDTO taskData);

    void delete(long id);
}
