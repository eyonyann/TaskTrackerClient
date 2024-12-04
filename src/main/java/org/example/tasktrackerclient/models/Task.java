package org.example.tasktrackerclient.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

//@Getter
//@Setter
//@AllArgsConstructor
//@NoArgsConstructor
//public class Task {
//
//    @JsonProperty("id")
//    private Long id;
//
//    @JsonProperty("name")
//    private String name;
//
//    @JsonProperty("description")
//    private String description;
//
//    @JsonProperty("status")
//    private String status;
//
//    @JsonProperty("priority")
//    private String priority;
//
//    @JsonProperty("deadline")
//    private LocalDateTime deadline;
//
//    @JsonProperty("endTime")
//    private LocalDateTime endTime;
//
//    @JsonProperty("assignId")
//    private Long developerId;
//
//    @Override
//    public String toString() {
//        return "Task{" +
//                "id=" + id +
//                ", name='" + name + '\'' +
//                ", description='" + description + '\'' +
//                ", status='" + status + '\'' +
//                ", priority='" + priority + '\'' +
//                ", deadline=" + deadline +
//                ", endTime=" + endTime +
//                ", assignId=" + developerId +
//                '}';
//    }
//
//}


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String priority;
    private LocalDateTime deadline;
    private LocalDateTime endTime;
    private LocalDateTime checkTime;
    private Long developerId;
    private Long testerId;
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", deadline=" + deadline +
                ", endTime=" + endTime +
                ", checkTime=" + checkTime +
                ", developerId=" + developerId +
                ", testerId=" + testerId +
                '}';
    }
}