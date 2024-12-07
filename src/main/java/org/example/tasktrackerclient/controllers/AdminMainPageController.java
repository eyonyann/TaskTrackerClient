package org.example.tasktrackerclient.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.tasktrackerclient.SessionManager;
import org.example.tasktrackerclient.TaskTrackerClient;
import org.example.tasktrackerclient.dtos.ProjectStatisticsDTO;
import org.example.tasktrackerclient.dtos.TaskCompletionDTO;
import org.example.tasktrackerclient.dtos.UserStatisticsDTO;
import org.example.tasktrackerclient.models.Project;
import org.example.tasktrackerclient.models.Task;
import org.example.tasktrackerclient.models.User;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;

public class AdminMainPageController {

    @FXML
    public VBox chartVBox;
    @FXML
    private HBox profileBox;

    @FXML
    private Button taskPageButton;

    @FXML
    private Button projectPageButton;

    @FXML
    private Button mainPageButton;

    @FXML
    private Label profileNameLabel;

    @FXML
    private VBox userInfoTableVBox;

    @FXML
    private Label profileRoleLabel;

    @FXML
    private LineChart<String, Number> lineChart;

    public void initialize() {
        initializeChartWithCompletionData();
        initializeUserInfo();
        initializeUserTable();
    }
    private void initializeUserTable() {
        List<User> users = fetchUsersFromServer();
        if (users.isEmpty()) {
            System.err.println("Список пользователей пуст.");
            return;
        }
        List<Project> projects = fetchAllProjects();
        if (projects.isEmpty()) {
            System.err.println("Список проектов пуст.");
            return;
        }

        String[] projectNames = projects.stream()
                .map(Project::getName)
                .toArray(String[]::new);

        userInfoTableVBox.getChildren().clear();

        HBox header = new HBox(10);
        header.setPadding(new Insets(5));
        header.getChildren().addAll(
                createHeadingLabel("ID", 20),
                createHeadingLabel("Имя Фамилия", 200),
                createHeadingLabel("Логин", 120),
                createHeadingLabel("Роль", 120),
                createHeadingLabel("Проект", 120),
                createHeadingLabel("Удалить", 80)
        );
        userInfoTableVBox.getChildren().add(header);

        for (User user : users) {
            HBox row = new HBox(10);
            row.setPadding(new Insets(5));

            Project project = fetchProjectInfoByUserId(user.getId());

            // Роль ChoiceBox
            ChoiceBox<String> roleChoiceBox = createChoiceBox(
                    new String[]{"DEVELOPER", "TESTER", "ADMIN"}, user.getRole().toString(), 120);
            roleChoiceBox.setOnAction(event -> handleRoleChange(user, roleChoiceBox.getValue()));

            // Проект ChoiceBox
            ChoiceBox<String> projectChoiceBox = createChoiceBox(
                    projectNames, project != null ? project.getName() : "Без проекта", 120);
            projectChoiceBox.setOnAction(event -> handleProjectChange(user, projectChoiceBox.getValue()));

            row.getChildren().addAll(
                    createLabel(String.valueOf(user.getId()), 20),
                    createLabel(user.getFullname(), 200),
                    createLabel(user.getUsername(), 120),
                    roleChoiceBox,
                    projectChoiceBox,
                    createDeleteLabel(user.getId(), 60)
            );
            userInfoTableVBox.getChildren().add(row);
        }
    }


    private void handleRoleChange(User user, String newRole) {
        if (user.getRole().toString().equals(newRole)) {
            return; // Если роль не изменилась, ничего не делаем.
        }

        boolean confirm = showConfirmationDialog("Вы уверены, что хотите изменить роль? Все задачи пользователя будут перемещены в BACKLOG.");
        if (!confirm) {
            return; // Если админ отменил действие.
        }

        // Логика перемещения задач в BACKLOG
        moveTasksToBacklog(user);

        updateUserRole(user.getId(), newRole);

    }


    private void handleProjectChange(User user, String newProject) {
        boolean confirm = showConfirmationDialog("Вы уверены, что хотите изменить проект? Все задачи пользователя будут перемещены в BACKLOG.");
        if (!confirm) {
            return; // Если админ отменил действие.
        }

        // Логика перемещения задач в BACKLOG
        moveTasksToBacklog(user);

        // Обновление проекта на сервере
        updateUserProject(user.getId(), newProject);
    }

    private boolean showConfirmationDialog(String message) {
        // Здесь можно использовать стандартное окно подтверждения JavaFX (например, Alert).
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение");
        alert.setContentText(message);

        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void moveTasksToBacklog(User user) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/tasks/move_to_backlog/" + user.getId()))
                    .header("Authorization", SessionManager.getAuthToken())
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Ошибка при перемещении задач в BACKLOG: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateUserRole(Long userId, String newRole) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String body = "{\"role\": \"" + newRole + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/users/" + userId + "/role"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Ошибка при обновлении роли: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUserProject(Long userId, String newProject) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String body = "{\"projectName\": \"" + newProject + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/users/" + userId + "/project"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Ошибка при обновлении проекта: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Label createHeadingLabel(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-alignment: center-left;");
        return label;
    }

    private Label createLabel(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle("-fx-font-size: 16; -fx-alignment: center-left;");
        return label;
    }

    private ChoiceBox<String> createChoiceBox(String[] options, String selected, double width) {
        ChoiceBox<String> choiceBox = new ChoiceBox<>(FXCollections.observableArrayList(options));
        choiceBox.setValue(selected);
        choiceBox.setPrefWidth(width);
        choiceBox.getStyleClass().add("user_info_choice_box");
        return choiceBox;
    }

    private Label createDeleteLabel(Long userId, double width) {
        Label deleteLabel = new Label("Удалить");
        deleteLabel.setPrefWidth(width);
        deleteLabel.setStyle("-fx-text-fill: rgba(255, 0, 0, 0.69);; -fx-text-alignment: center; -fx-alignment: center;");
        deleteLabel.setOnMouseClicked(event -> deleteUser(userId));
        return deleteLabel;
    }

    private void deleteUser(Long userId) {
        // Показываем предупреждающий диалог
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Удаление пользователя");
        alert.setContentText("Вы уверены, что хотите удалить этого пользователя? Это действие необратимо.");

        // Ожидаем ответа от пользователя
        if (alert.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:8080/api/users/" + userId + "/delete"))
                        .header("Authorization", SessionManager.getAuthToken())
                        .DELETE()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    System.out.println("Пользователь удален: " + response.body());
                    initializeUserTable(); // Обновляем таблицу пользователей после удаления
                } else {
                    System.err.println("Ошибка при удалении пользователя: " + response.statusCode() + " - " + response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    private void initializeChartWithCompletionData() {
        // Очистка VBox перед добавлением нового графика
        chartVBox.getChildren().clear();

        // Получаем все проекты и их статистику
        List<ProjectStatisticsDTO> projectStatistics = fetchAllProjectsStatistics(); // Метод, который запрашивает данные с API

        for (ProjectStatisticsDTO projectStat : projectStatistics) {
            // Подготовим данные для графика для каждого проекта
            Map<Long, Map<LocalDate, Integer>> userTaskCompletion = new HashMap<>();

            // Заполняем данные для графика
            for (UserStatisticsDTO userStat : projectStat.getUsers()) {
                for (TaskCompletionDTO taskCompletion : userStat.getCompletedTasks()) {
                    Long userId = userStat.getUserId();
                    LocalDate date = taskCompletion.getDate().toLocalDate();
                    int taskCount = taskCompletion.getCount();

                    userTaskCompletion
                            .computeIfAbsent(userId, k -> new HashMap<>())
                            .merge(date, taskCount, Integer::sum);
                }
            }

            // Создаем оси для графика
            CategoryAxis xAxis = new CategoryAxis();
            xAxis.setLabel("Дата");

            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Количество задач");
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(10); // Поменяйте при необходимости
            yAxis.setTickUnit(2);

            // Создаем LineChart для каждого проекта
            LineChart<String, Number> dynamicLineChart = new LineChart<>(xAxis, yAxis);
            dynamicLineChart.setTitle("Прогресс задач в проекте: " + projectStat.getProjectName());
            dynamicLineChart.getStyleClass().add("lineChart");
            dynamicLineChart.setMinHeight(400);
            dynamicLineChart.setPrefWidth(800);
            // Для каждого пользователя создаем свою линию на графике
            for (Map.Entry<Long, Map<LocalDate, Integer>> entry : userTaskCompletion.entrySet()) {
                LineChart.Series<String, Number> series = new LineChart.Series<>();
                series.setName("Пользователь " + entry.getKey());

                // Добавляем данные по датам для каждого пользователя
                for (Map.Entry<LocalDate, Integer> dateEntry : entry.getValue().entrySet()) {
                    series.getData().add(new LineChart.Data<>(dateEntry.getKey().toString(), dateEntry.getValue()));
                }

                dynamicLineChart.getData().add(series);
            }

            Region spacingRegion = new Region();
            spacingRegion.setMinHeight(15);
            // Добавляем график в chartVBox
            chartVBox.getChildren().addAll(dynamicLineChart, spacingRegion);
        }
    }

    private List<ProjectStatisticsDTO> fetchAllProjectsStatistics() {
        try {
            // Создаем объект HttpClient
            HttpClient client = HttpClient.newHttpClient();

            // Создаем запрос
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/projects/statistics"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            // Отправляем запрос и получаем ответ
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Проверяем статус ответа
            if (response.statusCode() == 200) {
                // Настраиваем ObjectMapper
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule()); // Регистрация модуля для LocalDateTime
                //objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // Читаем даты как строки ISO 8601

                // Десериализация ответа
                return objectMapper.readValue(response.body(), new TypeReference<List<ProjectStatisticsDTO>>() {});
            } else {
                System.out.println("Ошибка при получении данных: " + response.statusCode());
                return Collections.emptyList();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }




    private List<Task> fetchTasksByProject(Long projectId) {
        List<Task> tasks = new ArrayList<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/tasks/" + projectId))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                tasks = mapper.readValue(response.body(), new TypeReference<List<Task>>() {});
            } else {
                System.err.println("Ошибка при получении задач: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tasks;
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



    private Project fetchProjectInfoByUserId(Long userId) {
        Project project = null;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/projects/" + userId + "/project_info"))
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

    private List<User> fetchUsersFromServer() {
        List<User> users = new ArrayList<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/users"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                users = mapper.readValue(response.body(), new TypeReference<List<User>>() {});
            } else {
                System.err.println("Ошибка при получении списка пользователей: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    private void updateUserInfoOnServer(User user) throws IOException, InterruptedException, URISyntaxException {
        User updatedUser = new User();
        updatedUser.setId(fetchUserInfoFromServer().getId());

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
        } else {
            System.err.println("Ошибка при обновлении информации о пользователе: " + updateResponse.statusCode());
        }
    }

}