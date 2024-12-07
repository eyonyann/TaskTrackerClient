package org.example.tasktrackerclient.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskTableRow extends Task {
    private Long id;
    private String projectName;
    private String name;
    private String description;
    private String priority;
    private String status;
    private Long developerId;
    private Long testerId;
    private LocalDateTime deadline;
    private LocalDateTime endTime;
    private LocalDateTime checkTime;

    public TaskTableRow(Task task, String projectName) {
        this.id = task.getId();
        this.projectName = projectName;
        this.name = task.getName();
        this.description = task.getDescription();
        this.priority = task.getPriority();
        this.status = task.getStatus();
        this.developerId = task.getDeveloperId();
        this.testerId = task.getTesterId();
        this.deadline = task.getDeadline();
        this.endTime = task.getEndTime();
        this.checkTime = task.getCheckTime();
    }

    // Геттеры и сеттеры
}
