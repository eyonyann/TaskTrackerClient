package org.example.tasktrackerclient;

import lombok.Getter;
import lombok.Setter;

public class SessionManager {
    @Getter
    @Setter
    private static String authToken;

    public static void clearAuthToken() {
        authToken = null;
    }
}
