package hexlet.code.app.service;

import hexlet.code.app.dto.TaskCreateDTO;
import hexlet.code.app.dto.TaskDTO;
import hexlet.code.app.dto.TaskUpdateDTO;
import hexlet.code.app.exception.ResourceNotFoundException;
import hexlet.code.app.mapper.TaskMapper;
import hexlet.code.app.model.Task;
import hexlet.code.app.repository.LabelRepository;
import hexlet.code.app.repository.TaskRepository;
import hexlet.code.app.repository.TaskStatusRepository;
import hexlet.code.app.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service("taskService")
public class TaskServiceImpl implements TaskService {

    private static final String TASK_NOT_FOUND = "Task with id %d not found";
    private static final String TASK_STATUS_NOT_FOUND = "TaskStatus '%s' not found";
    private static final String DEFAULT_TASK_STATUS_NOT_FOUND = "Default status 'draft' not found";
    private static final String ASSIGNEE_NOT_FOUND = "Assignee with id %d not found";

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final UserRepository userRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final LabelRepository labelRepository;

    public TaskServiceImpl(TaskRepository taskRepository,
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

    @Override
    public List<TaskDTO> getAll(Specification<Task> spec, Pageable pageable) {
        var tasks = taskRepository.findAll(spec, pageable);
        return tasks.stream().map(taskMapper::map).toList();
    }

    @Override
    public TaskDTO getTask(long id) {
        var task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND.formatted(id)));
        return taskMapper.map(task);
    }

    @Override
    public TaskDTO create(TaskCreateDTO taskData) {
        var task = taskMapper.map(taskData);

        var statusSlug = taskData.getStatus() != null ? taskData.getStatus() : "draft";
        var taskStatus = taskStatusRepository.findTaskStatusBySlug(statusSlug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        taskData.getStatus() != null
                                ? TASK_STATUS_NOT_FOUND.formatted(taskData.getStatus())
                                : DEFAULT_TASK_STATUS_NOT_FOUND));

        task.setTaskStatus(taskStatus);

        if (taskData.getLabelIds() != null) {
            var labels = new HashSet<>(labelRepository.findAllById(taskData.getLabelIds()));
            task.setLabels(labels);
        }

        if (taskData.getAssigneeId() != null) {
            var assigneeId = taskData.getAssigneeId();
            var user = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ASSIGNEE_NOT_FOUND.formatted(assigneeId)));
            task.setAssignee(user);
        }

        taskRepository.save(task);
        return taskMapper.map(task);
    }

    @Override
    public TaskDTO update(long id, TaskUpdateDTO taskData) {
        var task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND.formatted(id)));

        if (taskData.getStatus() != null) {
            var taskStatus = taskStatusRepository.findTaskStatusBySlug(taskData.getStatus())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            TASK_STATUS_NOT_FOUND.formatted(taskData.getStatus())));
            task.setTaskStatus(taskStatus);
        }

        if (taskData.getAssigneeId() != null) {
            var assigneeId = taskData.getAssigneeId();
            var user = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ASSIGNEE_NOT_FOUND.formatted(assigneeId)));
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

    @Override
    public void delete(long id) {
        var task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND.formatted(id)));
        taskRepository.delete(task);
    }
}
