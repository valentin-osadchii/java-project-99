package hexlet.code.app.service;

import hexlet.code.app.dto.TaskCreateDTO;
import hexlet.code.app.dto.TaskDTO;
import hexlet.code.app.exception.ResourceNotFoundException;
import hexlet.code.app.mapper.TaskMapper;
import hexlet.code.app.repository.TaskRepository;
import hexlet.code.app.repository.TaskStatusRepository;
import hexlet.code.app.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    private final UserRepository userRepository;
    private final TaskStatusRepository taskStatusRepository;


    public TaskService(TaskRepository taskRepository,
                       TaskMapper taskMapper,
                       UserRepository userRepository,
                       TaskStatusRepository taskStatusRepository) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.userRepository = userRepository;
        this.taskStatusRepository = taskStatusRepository;
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

    public TaskDTO create(TaskCreateDTO taskData) {
        var task = taskMapper.map(taskData);

        var statusSlug = taskData.getStatus() != null ? taskData.getStatus() : "draft";
        var taskStatus = taskStatusRepository.findTaskStatusBySlug(statusSlug);

        if (taskStatus == null) {
            var message = taskData.getStatus() != null
                    ? "TaskStatus '" + taskData.getStatus() + "' not found"
                    : "Default status 'draft' not found";
            throw new ResourceNotFoundException(message);
        }

        task.setTaskStatus(taskStatus);
        taskRepository.save(task);
        return taskMapper.map(task);
    }

    public void delete(long id) {
        var task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));
        taskRepository.delete(task);
    }


}
