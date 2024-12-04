package org.example.tasktrackerclient.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    private String username;
    private String fullname;
    private String password;
    private String salt;
    private Role role;

    @JsonIgnore
    private Project project;

    public enum Role {
        ADMIN,
        DEVELOPER,
        TESTER
    }
}
