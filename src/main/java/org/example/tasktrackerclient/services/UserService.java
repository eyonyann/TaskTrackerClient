package org.example.tasktrackerclient.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.tasktrackerclient.SessionManager;
import org.example.tasktrackerclient.models.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class UserService {

    private static final String USERS_URL = "http://localhost:8080/api/users";


    public Boolean isThisUserInSystem(Long userId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(USERS_URL + "/is_this_user/" + userId))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(response.body(), Boolean.class);
            } else {
                System.err.println("Ошибка при проверке пользователя: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public User fetchUserInfoFromServer(Long userId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(USERS_URL + "/user_info?userId=" + userId))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(response.body(), User.class);
            } else {
                System.err.println("Ошибка при получении информации о пользователе: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
