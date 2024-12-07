package org.example.tasktrackerclient.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.example.tasktrackerclient.SessionManager;
import org.example.tasktrackerclient.TaskTrackerClient;
import org.example.tasktrackerclient.models.Project;
import org.example.tasktrackerclient.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

public class AdminProjectsPageController {
    @FXML
    public Label profileNameLabel;
    @FXML
    public Label profileRoleLabel;
    @FXML
    private HBox profileBox;

    @FXML
    private Button taskPageButton;
    @FXML
    private Button projectPageButton;
    @FXML
    private Button mainPageButton;

    @FXML
    public TextField projectName;
    @FXML
    public DatePicker deadlineDate;
    @FXML
    public TextArea projectDescription;
    @FXML
    public Button createButton;
    @FXML
    public Button cancelButton;
    @FXML
    public VBox projectsVBox;

    @FXML
    public void initialize() {
        initializeUserInfo();
        initializeProjects();
    }

    private void initializeProjects() {
        projectsVBox.getChildren().clear();
        List<Project> projects = fetchProjectsFromServer();
        if (projects != null) {
            for (Project project : projects) {
                VBox projectBox = createProjectBox(project);
                Region spacingRegion = new Region();
                spacingRegion.setMinHeight(10); // Устанавливаем высоту отступа
                projectsVBox.getChildren().addAll(projectBox, spacingRegion);
            }
        }
    }


    private List<Project> fetchProjectsFromServer() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/projects"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                return Arrays.asList(mapper.readValue(response.body(), Project[].class));
            } else {
                System.err.println("Ошибка при получении списка проектов: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private VBox createProjectBox(Project project) {

        // Создание контейнера для проекта
        VBox projectBox = new VBox();
        projectBox.setPrefSize(800, 74);
        projectBox.setStyle("-fx-background-radius: 7; -fx-border-radius: 7; -fx-background-color: rgba(255, 255, 255, 0.3); -fx-padding: 7;");

        // Название проекта
        Label titleLabel = new Label(project.getName());
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));

        // Дедлайн
        Label deadlineLabel = new Label(project.getDeadline());
        deadlineLabel.setTextFill(Color.WHITE);
        deadlineLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        // Описание проекта
        String description = renderHtmlToPlainText(project.getDescription());
        if (description.length() > 300) {
            description = description.substring(0, 300) + "...";
        }
        Text descriptionText = new Text(description);
        descriptionText.setFill(Color.WHITE);
        TextFlow descriptionFlow = new TextFlow(descriptionText);

        // Добавление компонентов в контейнер
        projectBox.getChildren().addAll(titleLabel, deadlineLabel, descriptionFlow);
        projectBox.setSpacing(5); // Отступы между элементами



        return projectBox;
    }

    private String renderHtmlToPlainText(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return "";
        }

        Document doc = Jsoup.parse(htmlContent);
        return doc.text(); // Извлекаем текст из HTML
    }

    public void MainPage(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("AdminMainPage.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        ((Stage) mainPageButton.getScene().getWindow()).close();
    }

    public void ProjectPage(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("AdminProjectsPage.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        ((Stage) projectPageButton.getScene().getWindow()).close();
    }

    public void TasksPage(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("AdminTasksPage.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        ((Stage) taskPageButton.getScene().getWindow()).close();
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

    @FXML
    public void handleCreateButtonClick(ActionEvent event) {
        String name = projectName.getText().trim();
        String deadline = (deadlineDate.getValue() != null)
                ? deadlineDate.getValue().toString() + "T00:00:00"
                : null;

        String description = projectDescription.getText().trim();

        if (name.isEmpty() || deadline == null || description.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Ошибка", "Все поля должны быть заполнены!");
            return;
        }

        // Преобразование описания в HTML
        String htmlDescription = description.replace("\n\n", "<p>").replace("\n", " ");

        // Создание JSON-объекта для запроса
        Project project = new Project(name, deadline, htmlDescription);
        ObjectMapper mapper = new ObjectMapper();

        try {
            String json = mapper.writeValueAsString(project);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/projects"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Проект успешно создан!");
                clearForm();
                initializeProjects();
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось создать проект: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Произошла ошибка при отправке данных на сервер.");
        }
    }

    @FXML
    public void handleCancelButtonClick(ActionEvent event) {
        clearForm();
    }

    private void clearForm() {
        projectName.clear();
        deadlineDate.setValue(null);
        projectDescription.clear();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
}
