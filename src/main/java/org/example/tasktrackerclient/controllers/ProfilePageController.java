package org.example.tasktrackerclient.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.example.tasktrackerclient.SessionManager;
import org.example.tasktrackerclient.TaskTrackerClient;
import org.example.tasktrackerclient.models.Project;
import org.example.tasktrackerclient.models.Task;
import org.example.tasktrackerclient.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProfilePageController {

    @FXML
    private HBox profileBox;

    @FXML
    private Label bigProfileRoleLabel;
    @FXML
    public TextField fullnameField;
    @FXML
    public TextField usernameField;
    @FXML
    public PasswordField currentPasswordField;
    @FXML
    public PasswordField newPasswordField;
    @FXML
    public PasswordField confirmPasswordField;
    @FXML
    public Button saveButton;
    @FXML
    public Button signOutButton;


    @FXML
    private Button taskPageButton;
    @FXML
    private Button projectPageButton;
    @FXML
    private Button mainPageButton;


    @FXML
    private Label profileNameLabel;
    @FXML
    private Label profileRoleLabel;


    public void initialize() {
        initializeUserInfo();
        initializeProfileFields();
    }



    private void initializeUserInfo() {
        User user = fetchUserInfoFromServer();
        if (user != null) {
            profileNameLabel.setText(user.getFullname());
            profileRoleLabel.setText(user.getRole().name());
        } else {
            System.err.println("Не удалось загрузить данные пользователя");
        }
    }


    private void initializeProfileFields() {
        User user = fetchUserInfoFromServer();
        if (user != null) {
            fullnameField.setText(user.getFullname());
            usernameField.setText(user.getUsername());
            bigProfileRoleLabel.setText(user.getRole().name());
        } else {
            System.err.println("Не удалось загрузить данные пользователя");
        }
    }



    private User fetchUserInfoFromServer() {
        User user = null;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/users/user_info"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(request.headers());
            System.out.println(response.body());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                user = mapper.readValue(response.body(), User.class);
            } else {
                System.err.println("Ошибка при получении информации о пользователе: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }



    public void MainPage(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("MainPage.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        ((Stage) mainPageButton.getScene().getWindow()).close();
    }

    public void ProjectPage(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("ProjectPage.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        ((Stage) projectPageButton.getScene().getWindow()).close();
    }

    public void TasksPage(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("TaskPage.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        ((Stage) taskPageButton.getScene().getWindow()).close();
    }


    @FXML
    private void handleProfileBoxClick(MouseEvent event) throws IOException {
        System.out.println("Profile box clicked!");

        FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("ProfilePage.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        ((Stage) profileBox.getScene().getWindow()).close();
    }

    public void save(ActionEvent actionEvent) {
        try {
            String currentPassword = currentPasswordField.getText();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            // Проверяем, нужно ли менять пароль
            boolean isPasswordChangeRequested = !newPassword.isEmpty() || !confirmPassword.isEmpty();

            // 1. Если пароль не меняется, обновляем только данные профиля
            if (!isPasswordChangeRequested) {
                updateUserInfoOnServer(null); // передаем null, если пароль не меняется
                return;
            }

            // 2. Проверка текущего пароля
            if (currentPassword.isEmpty()) {
                System.err.println("Введите текущий пароль");
                return;
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest checkPasswordRequest = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/users/verify_password"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .POST(HttpRequest.BodyPublishers.ofString(currentPassword))
                    .build();

            HttpResponse<String> checkPasswordResponse = client.send(checkPasswordRequest, HttpResponse.BodyHandlers.ofString());
            if (checkPasswordResponse.statusCode() != 200) {
                System.err.println("Неверный текущий пароль");
                return;
            }

            // 3. Проверка нового пароля и подтверждения
            if (!newPassword.equals(confirmPassword)) {
                System.err.println("Пароль не соответствует подтверждению");
                return;
            }

            // 4. Обновление данных на сервере с новым паролем
            updateUserInfoOnServer(newPassword);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUserInfoOnServer(String newPassword) throws IOException, InterruptedException, URISyntaxException {
        User updatedUser = new User();
        updatedUser.setId(fetchUserInfoFromServer().getId());
        updatedUser.setUsername(usernameField.getText());
        updatedUser.setFullname(fullnameField.getText());
        if (newPassword != null) {
            updatedUser.setPassword(newPassword); // устанавливаем новый пароль, если он меняется
        }

        ObjectMapper mapper = new ObjectMapper();
        String userJson = mapper.writeValueAsString(updatedUser);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest updateRequest = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/api/users/user_info"))
                .header("Authorization", SessionManager.getAuthToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(userJson))
                .build();

        HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
        if (updateResponse.statusCode() == 200) {
            System.out.println("Информация пользователя успешно обновлена");
            FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("ProfilePage.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();

            ((Stage) profileBox.getScene().getWindow()).close();
        } else {
            System.err.println("Ошибка при обновлении информации о пользователе: " + updateResponse.statusCode());
        }
    }



    public void signOut(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("SignIn.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        ((Stage) profileBox.getScene().getWindow()).close();
    }
}