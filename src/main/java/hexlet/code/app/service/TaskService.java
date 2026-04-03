package hexlet.code.app.service;

import hexlet.code.app.dto.TaskCreateDTO;
import hexlet.code.app.dto.TaskDTO;
import hexlet.code.app.dto.TaskUpdateDTO;
import hexlet.code.app.exception.ResourceNotFoundException;
import hexlet.code.app.mapper.TaskMapper;
import hexlet.code.app.repository.LabelRepository;
import hexlet.code.app.repository.TaskRepository;
import hexlet.code.app.repository.TaskStatusRepository;
import hexlet.code.app.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final UserRepository userRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final LabelRepository labelRepository;


    public TaskService(TaskRepository taskRepository,
                       TaskMapper taskMapper,
                       UserRepository userRepository,
                       TaskStatusRepository taskStatusRepository,
                       LabelRepository labelRepository) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.userRepository = userRepository;
        this.taskStatusRepository = taskStatusRepository;
        this.labelRepository = labelRepository;
    }

    public List<TaskDTO> getAll() {
        var taskStatuses = taskRepository.findAll();
        return taskStatuses.stream().map(taskMapper::map).toList();
    }

    public TaskDTO getTask(long id) {
        var taskStatus = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not Found"));
        return taskMapper.map(taskStatus);
    }

    public TaskDTO create(TaskCreateDTO taskData) {
        var task = taskMapper.map(taskData);

        var statusSlug = taskData.getStatus() != null ? taskData.getStatus() : "draft";
        var taskStatus = taskStatusRepository.findTaskStatusBySlug(statusSlug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        taskData.getStatus() != null
                                ? "TaskStatus '" + taskData.getStatus() + "' not found"
                                : "Default status 'draft' not found"));

        task.setTaskStatus(taskStatus);

        if (taskData.getLabelIds() != null) {
            var labels = new HashSet<>(labelRepository.findAllById(taskData.getLabelIds()));
            task.setLabels(labels);
        }

        taskRepository.save(task);
        return taskMapper.map(task);
    }


    public TaskDTO update(long id, TaskUpdateDTO taskData) {
        var task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));

        if (taskData.getStatus() != null) {
            var taskStatus = taskStatusRepository.findTaskStatusBySlug(taskData.getStatus())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "TaskStatus '" + taskData.getStatus() + "' not found"));
            task.setTaskStatus(taskStatus);
        }

        if (taskData.getAssigneeId() != null) {
            var assigneeId = taskData.getAssigneeId();
            var user = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Assignee with id " + assigneeId + " not found"));
            task.setAssignee(user);
        }

        if (taskData.getLabelIds() != null) {
            var labels = new HashSet<>(labelRepository.findAllById(taskData.getLabelIds()));
            task.setLabels(labels);
        }

        taskMapper.update(taskData, task);
        taskRepository.save(task);
        return taskMapper.map(task);
    }

    public void delete(long id) {
        var task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));
        taskRepository.delete(task);
    }


}
