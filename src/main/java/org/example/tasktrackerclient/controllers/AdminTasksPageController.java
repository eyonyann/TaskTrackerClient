package org.example.tasktrackerclient.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
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
import org.example.tasktrackerclient.models.Task;
import org.example.tasktrackerclient.models.TaskTableRow;
import org.example.tasktrackerclient.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AdminTasksPageController {
    @FXML
    public Label profileNameLabel;
    @FXML
    public Label profileRoleLabel;
    @FXML
    public VBox tasksVBox;
    @FXML
    public ChoiceBox<String> taskPriority;
    @FXML
    public ChoiceBox<String> projectName;

    @FXML
    public TableView<Task> taskTable;
    @FXML
    public TableColumn<Task, Long> idColumn;
    @FXML
    public TableColumn<Task, String> projectColumn;
    @FXML
    public TableColumn<Task, String> nameColumn;
    @FXML
    public TableColumn<Task, String> descriptionColumn;
    @FXML
    public TableColumn<Task, String> priorityColumn;
    @FXML
    public TableColumn<Task, String> statusColumn;
    @FXML
    public TableColumn<Task, String> developerColumn;
    @FXML
    public TableColumn<Task, String> testerColumn;
    @FXML
    public TableColumn<Task, String> deadlineColumn;
    @FXML
    public TableColumn<Task, String> endTimeColumn;
    @FXML
    public TableColumn<Task, String> checkTimeColumn;



    @FXML
    private HBox profileBox;

    @FXML
    private Button taskPageButton;
    @FXML
    private Button projectPageButton;
    @FXML
    private Button mainPageButton;

    @FXML
    public TextField taskName;
    @FXML
    public DatePicker deadlineDate;
    @FXML
    public TextArea taskDescription;

    @FXML
    public Button createButton;
    @FXML
    public Button cancelButton;

    @FXML
    public void initialize() {
        taskPriority.getItems().addAll("HIGH", "MEDIUM", "LOW");
        taskPriority.setValue("MEDIUM");
        initializeProjects();
        initializeUserInfo();
        initializeTasks();
    }

    private void initializeProjects() {
        List<Project> projects = fetchAllProjects();
        if (projects.isEmpty()) {
            System.err.println("Список проектов пуст.");
            return;
        }
        projectName.getItems().clear();
        for (Project project : projects) {
            projectName.getItems().add(project.getName());
        }
    }

    private void initializeTasks() {
        List<Task> tasks = fetchTasksFromServer();

        if (tasks != null) {
            taskTable.getItems().clear();  // Очищаем таблицу перед добавлением данных

            // Создаем кэш для projectId -> projectName, чтобы минимизировать обращения к серверу
            Map<Long, String> projectNameCache = new HashMap<>();
            for (Task task : tasks) {
                System.out.println(task);
                Long projectId = task.getProjectId();
                if (!projectNameCache.containsKey(projectId)) {
                    projectNameCache.put(projectId, fetchProjectInfoById(projectId).getName());
                }
            }

            // Заполняем таблицу задач
            for (Task task : tasks) {
                String projectName = projectNameCache.get(task.getProjectId());
                taskTable.getItems().add(new TaskTableRow(task, projectName));
            }

            // Настраиваем колонки для отображения данных
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
            projectColumn.setCellValueFactory(new PropertyValueFactory<>("projectName"));
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            developerColumn.setCellValueFactory(new PropertyValueFactory<>("developerId"));
            testerColumn.setCellValueFactory(new PropertyValueFactory<>("testerId"));
            deadlineColumn.setCellValueFactory(new PropertyValueFactory<>("deadline"));
            endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
            checkTimeColumn.setCellValueFactory(new PropertyValueFactory<>("checkTime"));
        } else {
            System.err.println("Задачи не загружены");
        }
    }





    private List<Task> fetchTasksFromServer() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/tasks"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                return Arrays.asList(mapper.readValue(response.body(), Task[].class));
            } else {
                System.err.println("Ошибка при получении списка задач: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    private List<Project> fetchAllProjects() {
        List<Project> projects = new ArrayList<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/projects"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Request Headers: " + request.headers());
            System.out.println("Response Body: " + response.body());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                // Использование TypeReference для десериализации списка
                projects = mapper.readValue(response.body(), new TypeReference<List<Project>>() {});
            } else {
                System.err.println("Ошибка при получении списка проектов: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Ошибка при выполнении запроса к API проектов: ");
            e.printStackTrace();
        }
        return projects;
    }

    private Project fetchProjectInfoById(Long projectId) {
        try {
            // Создание HTTP клиента
            HttpClient client = HttpClient.newHttpClient();

            // Сборка HTTP запроса
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/projects/" + projectId))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            // Отправка запроса и обработка ответа
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Request Headers: " + request.headers());
            System.out.println("Response Body: " + response.body());

            if (response.statusCode() == 200) {
                // Успешный ответ: десериализация в объект Project
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()); // Для LocalDate
                return mapper.readValue(response.body(), Project.class);
            } else if (response.statusCode() == 404) {
                // Если проект не найден
                System.err.println("Проект с ID " + projectId + " не найден.");
                showAlert(Alert.AlertType.WARNING, "Ошибка", "Проект не найден.");
            } else if (response.statusCode() == 401) {
                // Если пользователь не авторизован
                System.err.println("Ошибка авторизации. Проверьте токен.");
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Вы не авторизованы.");
            } else {
                // Любые другие ошибки
                System.err.println("Неожиданная ошибка: HTTP " + response.statusCode());
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить данные проекта.");
            }
        } catch (Exception e) {
            // Обработка исключений
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Произошла ошибка при подключении к серверу.");
        }
        return null;
    }

    @FXML
    public void handleCreateButtonClick(ActionEvent event) {
        String status = "BACKLOG";
        String priority = taskPriority.getValue();
        Long projectId = fetchProjectIdByName(projectName.getValue());
        String name = taskName.getText().trim();
        String deadline = (deadlineDate.getValue() != null)
                ? deadlineDate.getValue().toString() + "T00:00:00"
                : null;

        String description = taskDescription.getText().trim();

        if (name.isEmpty() || deadline == null || description.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Ошибка", "Все поля должны быть заполнены!");
            return;
        }
        // Создание JSON-объекта для запроса
        Task task = new Task(name, projectId, deadline, description, status, priority);
        System.out.println(task);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // Регистрация модуля для LocalDateTime
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try {
            String json = mapper.writeValueAsString(task);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/tasks"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Задача успешно создан!");
                clearForm();
                initializeTasks();
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось создать задачу: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Произошла ошибка при отправке данных на сервер.");
        }
    }

    private Long fetchProjectIdByName(String projectName) {
        try {
            String encodedProjectName = URLEncoder.encode(projectName, StandardCharsets.UTF_8.toString()); // Кодирование параметра
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/projects/name?name=" + encodedProjectName)) // Вставка закодированного имени
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(response.body(), Long.class); // Предполагается, что сервер возвращает ID проекта в формате JSON
            } else {
                System.err.println("Ошибка при получении ID проекта: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Возвращаем null, если не удалось получить ID
    }



    @FXML
    public void handleCancelButtonClick(ActionEvent event) {
        clearForm();
    }

    private void clearForm() {
        taskName.clear();
        deadlineDate.setValue(null);
        taskDescription.clear();
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
