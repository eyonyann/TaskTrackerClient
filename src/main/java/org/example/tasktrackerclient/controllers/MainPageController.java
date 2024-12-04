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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.example.tasktrackerclient.SessionManager;
import org.example.tasktrackerclient.TaskTrackerClient;
import org.example.tasktrackerclient.models.Project;
import org.example.tasktrackerclient.models.Task;
import org.example.tasktrackerclient.models.User;
import org.example.tasktrackerclient.services.TaskService;
import org.example.tasktrackerclient.services.UserService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

public class MainPageController {

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
    private Label profileNameLabel;

    @FXML
    private Label profileRoleLabel;

    @FXML
    private LineChart<String, Number> lineChart;
    @FXML
    private VBox taskContainer;

    private List<Task> tasks;

    public void initialize() {
        initializeTasks();
        initializeChartWithCompletionData();
        displayTasks();
        initializeUserInfo();
        initializeProjectInfo();
    }

    private void initializeProjectInfo() {
        Project project = fetchProjectInfoFromServer();
        if (project != null) {
            projectNameLabel.setText(project.getName());
            projectDeadlineLabel.setText(project.getDeadline());
        } else {
            System.err.println("Не удалось загрузить данные проекта");
        }
    }


    private void initializeChartWithCompletionData() {
        // Получение текущего пользователя
        User currentUser = fetchCurrentUserInfo();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Сделанные задачи");

        Map<LocalDate, Long> completedTasksByDate;

        assert currentUser != null;
        if (User.Role.TESTER.equals(currentUser.getRole())) {
            // Для тестера - агрегация по checkTime и только задачи со статусом "DONE"
            completedTasksByDate = tasks.stream()
                    .filter(task -> "DONE".equals(task.getStatus()) && task.getCheckTime() != null)
                    .collect(Collectors.groupingBy(
                            task -> task.getCheckTime().toLocalDate(),
                            Collectors.counting()
                    ));
        } else if (User.Role.DEVELOPER.equals(currentUser.getRole())) {
            // Для девелопера - агрегация по endTime для задач со статусами "DONE" или "REVIEW"
            completedTasksByDate = tasks.stream()
                    .filter(task -> ("DONE".equals(task.getStatus()) || "REVIEW".equals(task.getStatus()))
                            && task.getEndTime() != null)
                    .collect(Collectors.groupingBy(
                            task -> task.getEndTime().toLocalDate(),
                            Collectors.counting()
                    ));
        } else {
            // Если роль неизвестна, оставляем пустую агрегацию
            completedTasksByDate = Collections.emptyMap();
        }

        // Сортировка данных по дате и добавление их в график
        completedTasksByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> series.getData().add(
                        new XYChart.Data<>(entry.getKey().toString(), entry.getValue())
                ));

        // Очистка и обновление графика
        lineChart.getData().clear();
        lineChart.getData().add(series);
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



    private void initializeTasks() {
        tasks = fetchTasksFromServer();
        System.out.println(tasks);
    }

    private List<Task> fetchTasksFromServer() {
        List<Task> tasks = new ArrayList<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/tasks"))
                    .header("Authorization", SessionManager.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(request.headers());
            System.out.println(response.body());

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


    private void displayTasks() {
        User user = fetchUserInfoFromServer();

        // Проверяем, есть ли информация о пользователе
        if (user == null) {
            System.out.println("Ошибка: пользователь не найден.");
            return;
        }

        List<Task> filteredTasks;

        // Фильтрация задач в зависимости от роли пользователя
        switch (user.getRole()) {
            case ADMIN:
                filteredTasks = tasks.stream()
                        .sorted(Comparator
                                .comparingInt(this::getPriorityValue)
                                .thenComparing(task -> LocalDateTime.parse(task.getDeadline().toString())))
                        .toList();
                break;

            case TESTER:
                filteredTasks = tasks.stream()
                        .filter(task -> "REVIEW".equals(task.getStatus()))
                        .sorted(Comparator
                                .comparingInt(this::getPriorityValue)
                                .thenComparing(task -> LocalDateTime.parse(task.getDeadline().toString())))
                        .toList();
                break;

            case DEVELOPER:
                filteredTasks = tasks.stream()
                        .filter(task -> "IN_PROGRESS".equals(task.getStatus()))
                        .sorted(Comparator
                                .comparingInt(this::getPriorityValue)
                                .thenComparing(task -> LocalDateTime.parse(task.getDeadline().toString())))
                        .toList();
                break;

            default:
                System.out.println("Ошибка: неподдерживаемая роль пользователя.");
                return;
        }

        // Очистка контейнера задач
        taskContainer.getChildren().clear();

        // Отображение задач
        for (Task task : filteredTasks) {
            VBox taskBox = createTaskBox(task);
            taskContainer.getChildren().add(taskBox);

            // Добавляем обработчик кликов по задаче
            taskBox.setOnMouseClicked(event -> openTaskWindow(task.getId()));
        }
    }


    private int getPriorityValue(Task task) {
        return switch (task.getPriority()) {
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
    }

    private VBox createTaskBox(Task task) {
        VBox taskBox = new VBox(5);
        taskBox.setPrefSize(800.0, 234.0);
        taskBox.getStyleClass().add("taskLabel");

        HBox titleBox = new HBox();
        titleBox.getChildren().addAll(
                createTaskNameLabel(task),
                createPriorityLabel(task)
        );

        HBox descriptionBox = new HBox(createDescriptionFlow(task));

        HBox actionBox = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
//        actionBox.getChildren().addAll(createStepAndDeadlineBox(task), spacer, createButtonBox(task));
        actionBox.getChildren().addAll(createStepAndDeadlineBox(task), spacer);

        taskBox.getChildren().addAll(titleBox, actionBox);

        return taskBox;
    }




    private Label createTaskNameLabel(Task task) {
        Label taskName = new Label(task.getName());
        taskName.getStyleClass().add("taskName");
        return taskName;
    }

    private Label createPriorityLabel(Task task) {
        Label priorityLabel = new Label("[" + task.getPriority() + "]");
        switch (task.getPriority()) {
            case "HIGH" -> priorityLabel.getStyleClass().add("highTaskPriority");
            case "MEDIUM" -> priorityLabel.getStyleClass().add("mediumTaskPriority");
            case "LOW" -> priorityLabel.getStyleClass().add("lowTaskPriority");
        }
        return priorityLabel;
    }

    private TextFlow createDescriptionFlow(Task task) {
        TextFlow descriptionFlow = new TextFlow();
        descriptionFlow.setPrefWidth(500.0);
        descriptionFlow.getStyleClass().add("taskDescription");
        Text descriptionText = new Text(task.getDescription());
        descriptionText.setFill(Color.WHITE);
        descriptionFlow.getChildren().add(descriptionText);
        return descriptionFlow;
    }

    private HBox createButtonBox(Task task) {
        HBox buttonBox = new HBox();
        Button declineButton = new Button("Отказаться");
        declineButton.setOnAction(event -> declineTask(task));
        Button doneButton = new Button("Сделано");
        doneButton.setOnAction(event -> completeTask(task));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonBox.getChildren().addAll(spacer, declineButton, doneButton);
        buttonBox.setSpacing(10);
        return buttonBox;
    }

    private VBox createStepAndDeadlineBox(Task task) {
        VBox stepAndDeadlineBox = new VBox();
        Label stepLabel = new Label("Статус: " + task.getStatus());
        stepLabel.getStyleClass().add("taskStep");

        Label deadlineLabel = new Label("Сделать до: " + task.getDeadline());
        deadlineLabel.getStyleClass().add("taskDeadline");

        Region spacer = new Region();
        stepAndDeadlineBox.getChildren().addAll(stepLabel, spacer, deadlineLabel);
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return stepAndDeadlineBox;
    }


    private void declineTask(Task task) {
        task.setStatus("BACKLOG");
        task.setEndTime(null);
        task.setDeveloperId(null);
        sendTaskUpdateToServer(task);
    }

    private void completeTask(Task task) {
        task.setStatus("REVIEW");
        task.setEndTime(LocalDateTime.now(ZoneOffset.UTC)); // Установка текущего времени в UTC
        sendTaskUpdateToServer(task);
    }


    private void sendTaskUpdateToServer(Task task) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String taskJson = mapper.writeValueAsString(task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/tasks/" + task.getId()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", SessionManager.getAuthToken())
                    .PUT(HttpRequest.BodyPublishers.ofString(taskJson))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Задача успешно обновлена на сервере: " + task.getName());
                initializeTasks();
                displayTasks();
                initializeChartWithCompletionData();
            } else {
                System.err.println("Ошибка при обновлении задачи: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private void openTaskWindow(Long taskId) {
        try {
            TaskWindowController controller = new TaskWindowController();
            controller.setTaskId(taskId);
            System.out.println("task idddddddddd " + taskId);

            FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("TaskWindow.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openTaskDescription(Task task) {
        System.out.println("Открытие описания для задачи: " + task.getName());
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