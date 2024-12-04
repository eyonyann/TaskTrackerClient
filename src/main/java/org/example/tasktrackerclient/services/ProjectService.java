package org.example.tasktrackerclient.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.tasktrackerclient.SessionManager;
import org.example.tasktrackerclient.models.Project;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProjectService {

    private static final String PROJECT_INFO_URL = "http://localhost:8080/api/projects/project_info";

    public Project fetchProjectInfo() {
        Project project = null;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(PROJECT_INFO_URL))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                project = mapper.readValue(response.body(), Project.class);
            } else {
                System.err.println("Ошибка при получении информации о проекте: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return project;
    }
}
