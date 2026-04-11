package hexlet.code.app.service;

import hexlet.code.app.dto.TaskStatusCreateDTO;
import hexlet.code.app.dto.TaskStatusDTO;
import hexlet.code.app.dto.TaskStatusUpdateDTO;

import java.util.List;

public interface TaskStatusService {

    List<TaskStatusDTO> getAll();

    TaskStatusDTO getTaskStatus(long id);

    TaskStatusDTO create(TaskStatusCreateDTO taskStatusData);

    TaskStatusDTO update(long id, TaskStatusUpdateDTO taskStatusData);

    void delete(long id);
}
