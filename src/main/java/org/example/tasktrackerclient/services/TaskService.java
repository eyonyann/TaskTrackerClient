package org.example.tasktrackerclient.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.tasktrackerclient.SessionManager;
import org.example.tasktrackerclient.models.Task;
import org.example.tasktrackerclient.models.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class TaskService {

    private static final String TASKS_URL = "http://localhost:8080/api/tasks";


    public Task fetchTaskInfoById(Long taskId) {
        if (taskId != null) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(TASKS_URL + "/task_info/" + taskId))
                        .header("Authorization", SessionManager.getAuthToken())
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.registerModule(new JavaTimeModule());
                    System.out.println("Успешное получение задач: " + mapper.readValue(response.body(), Task.class));
                    return mapper.readValue(response.body(),  Task.class);
                } else {
                    System.err.println("Ошибка при получении задачи: " + response.statusCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("taskId is null");
        }

        return null;

    }

    public void sendTaskUpdateToServer(Task task) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String taskJson = mapper.writeValueAsString(task);
            System.out.println("Отправляемый JSON: " + taskJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(TASKS_URL + task.getId()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", SessionManager.getAuthToken())
                    .PUT(HttpRequest.BodyPublishers.ofString(taskJson))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Задача успешно обновлена на сервере: " + task.getName());
            } else {
                System.err.println("Ошибка при обновлении задачи: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendAssignUpdate(Task task) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String taskJson = mapper.writeValueAsString(task);
            System.out.println("Отправляемый JSON: " + taskJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(TASKS_URL + task.getId() + "/assign"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", SessionManager.getAuthToken())
                    .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(task)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Задача успешно обновлена на сервере: поменяли разраба");
            } else {
                System.err.println("Ошибка при установлении разраба задачи: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
