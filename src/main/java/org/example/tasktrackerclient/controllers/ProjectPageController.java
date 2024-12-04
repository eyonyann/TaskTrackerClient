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

public class ProjectPageController {

    @FXML
    private HBox profileBox;

    @FXML
    private Button taskPageButton;

    @FXML
    private Button projectPageButton;

    @FXML
    private Button mainPageButton;

    @FXML
    private Label projectNameLabel;

    @FXML
    private Label projectDeadlineLabel;

    @FXML
    private Text projectDescriptionText;

    @FXML
    private Label profileNameLabel;

    @FXML
    private Label profileRoleLabel;


    public void initialize() {
        initializeUserInfo();
        initializeProjectInfo();
    }

    private void initializeProjectInfo() {
        Project project = fetchProjectInfoFromServer();
        if (project != null) {
            projectNameLabel.setText(project.getName());
            projectDeadlineLabel.setText(project.getDeadline());
            // Отображаем описание с HTML
            String htmlContent = project.getDescription();
            renderHtmlToText(htmlContent);  // Вызываем метод для рендеринга HTML
        } else {
            System.err.println("Не удалось загрузить данные проекта");
        }
    }


    private void renderHtmlToText(String htmlContent) {
        if (htmlContent != null) {
            // Используем библиотеку Jsoup для парсинга HTML
            Document doc = Jsoup.parse(htmlContent);
            Elements elements = doc.body().children();

            StringBuilder textBuilder = new StringBuilder(); // Строим строку текста

            for (Element element : elements) {
                switch (element.tagName()) {
                    case "b":
                    case "strong":
                        textBuilder.append(element.text()).append("\n");
                        break;
                    case "i":
                        textBuilder.append(element.text()).append("\n");
                        break;
                    case "u":
                        textBuilder.append(element.text()).append("\n");
                        break;
                    case "p":
                        textBuilder.append(element.text()).append("\n\n");  // Добавляем два символа новой строки после абзаца
                        break;
                    case "h1":
                        textBuilder.append(element.text()).append("\n");
                        break;
                    case "h2":
                        textBuilder.append(element.text()).append("\n");
                        break;
                    case "h3":
                        textBuilder.append(element.text()).append("\n");
                        break;
                    case "h4":
                        textBuilder.append(element.text()).append("\n");
                        break;
                    case "h5":
                        textBuilder.append(element.text()).append("\n");
                        break;
                    case "h6":
                        textBuilder.append(element.text()).append("\n");
                        break;
                    case "ul":
                    case "ol":
                        for (Element listItem : element.children()) {
                            textBuilder.append("• ").append(listItem.text()).append("\n");
                        }
                        break;
                    case "a":
                        textBuilder.append(element.text()).append("\n");
                        break;
                    default:
                        textBuilder.append(element.text()).append("\n");
                        break;
                }
            }

            // Устанавливаем текст в объект Text
            projectDescriptionText.setText(textBuilder.toString());
            projectDescriptionText.setFill(Color.WHITE);

            // Обновляем высоту контейнера с описанием в зависимости от размера текста
            projectDescriptionText.setWrappingWidth(800.0); // Ограничиваем ширину текста
        }
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

    private Project fetchProjectInfoFromServer() {
        Project project = null;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/projects/project_info"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(request.headers());
            System.out.println(response.body());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                project = mapper.readValue(response.body(), Project.class);
            } else {
                System.err.println("Ошибка при получении информации о пользователе: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return project;
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
        // Ваш код для обработки клика по HBox
        System.out.println("Profile box clicked!");
        // Здесь можно вызвать нужный метод или отправить сигнал, как вам нужно

        FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("ProfilePage.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        ((Stage) profileBox.getScene().getWindow()).close();
    }
}