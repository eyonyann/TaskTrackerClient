package org.example.tasktrackerclient.dtos;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectStatisticsDTO {
    private Long projectId;
    private String projectName;
    private List<UserStatisticsDTO> users;
}
