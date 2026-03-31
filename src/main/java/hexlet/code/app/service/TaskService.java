package hexlet.code.app.service;

import hexlet.code.app.dto.TaskDTO;
import hexlet.code.app.exception.ResourceNotFoundException;
import hexlet.code.app.mapper.TaskMapper;
import hexlet.code.app.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    public List<TaskDTO> getAll() {
        var taskStatuses = taskRepository.findAll();
        return taskStatuses.stream().map(taskMapper::map).toList();
    }

    public TaskDTO getTask(long id) {
        var taskStatus = taskRepository.findById((long) id)
                .orElseThrow(() -> new ResourceNotFoundException("Not Found"));
        return taskMapper.map(taskStatus);
    }


}
