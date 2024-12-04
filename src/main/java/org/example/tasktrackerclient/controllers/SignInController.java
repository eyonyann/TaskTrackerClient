package org.example.tasktrackerclient.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.tasktrackerclient.SessionManager;
import org.example.tasktrackerclient.TaskTrackerClient;
import org.example.tasktrackerclient.models.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SignInController {

    @FXML
    private Hyperlink signUpLink;

    @FXML
    private Button continueButton;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField passwordField;

    @FXML
    private void openSignUp(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(TaskTrackerClient.class.getResource("SignUp.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = new Stage();
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();

            ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMousePressed() {
        System.out.println("Кнопка нажата");
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), continueButton);
        scaleTransition.setToX(0.9);
        scaleTransition.setToY(0.9);
        scaleTransition.play();
    }

    @FXML
    private void handleMouseReleased() {
        System.out.println("Кнопка отпущена");
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), continueButton);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        scaleTransition.play();
    }

    @FXML
    private void signIn() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Ошибка: заполни все поля.");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonRequest = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/authenticate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String authToken = response.headers().firstValue("Authorization").orElse("");
                SessionManager.setAuthToken(authToken);
                System.out.println("Registration successful, token: " + authToken);
                User user = fetchCurrentUserInfo();

                if (user != null && user.getRole() == User.Role.ADMIN) {
                    FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("AdminMainPage.fxml"));
                    Scene scene = new Scene(loader.load());
                    Stage stage = new Stage();
                    stage.setResizable(false);
                    stage.setScene(scene);
                    stage.show();
                } else {
                    FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("MainPage.fxml"));
                    Scene scene = new Scene(loader.load());
                    Stage stage = new Stage();
                    stage.setResizable(false);
                    stage.setScene(scene);
                    stage.show();
                }
                ((Stage) continueButton.getScene().getWindow()).close();
            } else {
                System.out.println("Ошибка авторизации: " + response.statusCode());
                System.out.println("Тело ответа: " + response.body());
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private User fetchCurrentUserInfo() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/users/current"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(response.body(), User.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
