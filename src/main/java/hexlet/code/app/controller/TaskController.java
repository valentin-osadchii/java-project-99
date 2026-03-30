package hexlet.code.app.controller;

import hexlet.code.app.dto.TaskDTO;
import hexlet.code.app.dto.TaskStatusDTO;
import hexlet.code.app.mapper.TaskMapper;
import hexlet.code.app.repository.TaskRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

public class TaskController {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    public TaskController(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @GetMapping(path = "")
    @ResponseStatus(HttpStatus.OK)
    public List<TaskDTO> index(HttpServletResponse response) {
        var tasks = taskRepository.findAll().stream().map(taskMapper::map).toList();
        response.setHeader("X-Total-Count", String.valueOf(tasks.size()));
        return tasks;
    }

}
