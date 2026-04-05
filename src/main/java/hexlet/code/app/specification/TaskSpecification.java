package hexlet.code.app.specification;

import hexlet.code.app.dto.TaskParamsDTO;
import hexlet.code.app.model.Task;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;


@Component
public class TaskSpecification {
    public Specification<Task> build(TaskParamsDTO params) {
        return withAssigneeId(params.getAssigneeId())
                .and(withTitleLike(params.getTitleCont()))
                .and(withStatus(params.getStatus()))
                .and(withLabel(params.getLabelId()));
    }

    private Specification<Task> withAssigneeId(Long assigneeId) {
        return (root, query, cb) ->
                assigneeId == null ? cb.conjunction() : cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    private Specification<Task> withTitleLike(String titlePart) {
        return (root, query, cb) ->
                titlePart == null ? cb.conjunction() : cb.like(root.get("name"), "%" + titlePart + "%");
    }

    private Specification<Task> withStatus(String status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("taskStatus").get("slug"), status);
    }

    private Specification<Task> withLabel(Long labelId) {
        return (root, query, cb) ->
                labelId == null ? cb.conjunction() : cb.equal(root.join("labels").get("id"), labelId);
    }
}
