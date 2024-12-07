package org.example.tasktrackerclient.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Project {
    private Long id;
    private String name;
    private String description;
    private String deadline;

    @JsonIgnore
    private List<User> users;

    public Project(String name, String deadline, String htmlDescription) {
        this.name = name;
        this.description = htmlDescription;
        this.deadline = deadline;
    }
}
