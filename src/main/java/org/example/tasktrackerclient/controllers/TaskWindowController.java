package org.example.tasktrackerclient.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.tasktrackerclient.SessionManager;
import org.example.tasktrackerclient.models.Task;
import org.example.tasktrackerclient.models.User;
import org.example.tasktrackerclient.services.TaskService;
import org.example.tasktrackerclient.services.UserService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public class TaskWindowController {

    @FXML
    public Text descriptionText;
    @FXML
    public Label priorityLabel;
    @FXML
    public Label statusLabel;
    @FXML
    public Label deadlineLabel;
    @FXML
    public Label endTimeLabel;
    @FXML
    public Label developerLabel;
    @FXML
    public Text taskNameText;
    @FXML
    private VBox taskButtonsVBox;

    private static Long taskId;

    private TaskService taskService;
    private UserService userService;

    public TaskWindowController() {
        this.taskService = new TaskService();
        this.userService = new UserService();
    }


    public TaskWindowController(TaskService taskService, UserService userService) {
        this.taskService = taskService;
        this.userService = userService;
    }



    public void setTaskId(Long taskId) {
        TaskWindowController.taskId = taskId;
    }

    public void initialize() {
        initializeTaskInfo();
        initializeTaskButtons();
    }

    private void initializeTaskInfo() {
        Task task = fetchTaskInfoById(taskId);
        if (task != null) {
            taskNameText.setText(task.getName());
            descriptionText.setText(task.getDescription());
            priorityLabel.setText(String.valueOf(task.getPriority()));
            statusLabel.setText(String.valueOf(task.getStatus()));
            deadlineLabel.setText(String.valueOf(task.getDeadline()));
            endTimeLabel.setText(String.valueOf(task.getEndTime()));
            User developer = fetchUserInfoFromServer(task.getDeveloperId());
            developerLabel.setText(developer != null ? developer.getFullname() : "-");
        } else {
            System.out.println("Задача не найдена");
        }
    }

    private void initializeTaskButtons() {
        Task task = fetchTaskInfoById(taskId);
        User user = fetchCurrentUserInfo();

        if (task != null && user != null) {
            taskButtonsVBox.getChildren().clear(); // Очистка предыдущих кнопок

            switch (user.getRole()) {
                case DEVELOPER -> handleDeveloperActions(task, user);
                case TESTER -> handleTesterActions(task, user);
                default -> System.out.println("Роль пользователя не поддерживается для этой операции.");
            }
        } else {
            System.out.println("Задача или пользователь не найдены.");
            taskButtonsVBox.getChildren().clear();
        }
    }

    private void handleDeveloperActions(Task task, User user) {
        switch (task.getStatus()) {
            case "BACKLOG" -> {
                // Если задача в BACKLOG, разработчик может взять её
                if (task.getDeveloperId() == null || task.getDeveloperId().equals(user.getId())) {
                    createTakeTaskButton();
                }
            }
            case "IN_PROGRESS" -> {
                // Если задача в IN_PROGRESS и назначена на разработчика, доступны кнопки "Сделано" и "Вернуть"
                if (task.getDeveloperId() != null && task.getDeveloperId().equals(user.getId())) {
                    createActionButtons();
                }
            }
            case "REVIEW", "DONE" -> {
                // Для разработчика кнопки недоступны
                System.out.println("Для статусов REVIEW и DONE кнопки не отображаются для разработчика.");
            }
        }
    }

    private void handleTesterActions(Task task, User user) {
        switch (task.getStatus()) {
            case "REVIEW" -> {
                if (task.getTesterId() != null && task.getTesterId().equals(user.getId())) {
                    createActionButtons();
                }
                // Если задача в статусе REVIEW, тестировщик видит кнопки "Сделано" и "Вернуть"
            }
            case "BACKLOG", "IN_PROGRESS", "DONE" -> {
                // Для остальных статусов кнопки недоступны
                System.out.println("Кнопки не отображаются для тестировщика в текущем статусе задачи.");
            }
        }
    }


    private void createActionButtons() {
        HBox buttonBox = new HBox();
        Button markDoneButton = createButton("Сделано");
        markDoneButton.setOnAction(this::handleMarkDone);

        Region region = new Region();
        region.setMinWidth(10);
        region.setMaxWidth(10);

        Button refuseButton = createButton("Вернуть");
        refuseButton.setOnAction(this::handleRefuse);

        buttonBox.setAlignment(Pos.CENTER);

        buttonBox.getChildren().addAll(markDoneButton, region, refuseButton);
        taskButtonsVBox.getChildren().add(buttonBox);
    }

    private void createTakeTaskButton() {
        Button takeTaskButton = createButton("Взять");
        takeTaskButton.setOnAction(this::handleTakeTask);
        taskButtonsVBox.getChildren().add(takeTaskButton);
    }

    private Button createButton(String text) {
        Button button = new Button(text);
        button.setStyle("""
                -fx-background-color: white;
                -fx-text-fill: #131317;
                -fx-background-radius: 10;
                -fx-padding: 10 20;
                -fx-max-width: 300;
                -fx-font-size: 20;
                -fx-font-weight: bold;
                """);
        button.setMinWidth(145);
        button.setMaxWidth(145);
        return button;
    }

    private void handleMarkDone(ActionEvent event) {
        System.out.println("Кнопка 'Сделано' нажата для задачи ID: " + taskId);

        Task task = fetchTaskInfoById(taskId);
        User user = fetchCurrentUserInfo();

        if (task != null && user != null) {
            if (user.getRole() == User.Role.DEVELOPER) {
                // Логика для разработчика: статус на REVIEW, сохраняем endTime
                sendTesterUpdate(task);
                task.setStatus("REVIEW");
                task.setEndTime(LocalDateTime.now(ZoneOffset.of("+03:00")));
            } else if (user.getRole() == User.Role.TESTER) {
                // Логика для тестировщика: статус на DONE
                task.setStatus("DONE");
                task.setCheckTime(LocalDateTime.now(ZoneOffset.of("+03:00")));
                System.out.println("поменял время чека на в задаче " + task);
            }

            sendTaskUpdateToServer(task);
            System.out.println("Обновлённая задача: " + task);
            closeTaskWindow();
        }
    }

    private void handleRefuse(ActionEvent event) {
        System.out.println("Кнопка 'Вернуть' нажата для задачи ID: " + taskId);

        Task task = fetchTaskInfoById(taskId);
        User user = fetchCurrentUserInfo();

        if (task != null && user != null) {
            if (user.getRole() == User.Role.DEVELOPER) {
                // Логика для разработчика: возврат в BACKLOG, сбрасываем developerId
                task.setStatus("BACKLOG");
                task.setDeveloperId(null);
            } else if (user.getRole() == User.Role.TESTER) {
                // Логика для тестировщика: возврат в IN_PROGRESS, сбрасываем endTime
                task.setStatus("IN_PROGRESS");
                task.setEndTime(null);
            }

            sendTaskUpdateToServer(task);
            System.out.println("Обновлённая задача: " + task);
            closeTaskWindow();
        }
    }


    private void handleTakeTask(ActionEvent event) {
        System.out.println("Кнопка 'Взять' нажата для задачи ID: " + taskId);
        // Реализация взятия задачи пользователем на сервере

        Task task = fetchTaskInfoById(taskId);
        if (task != null) {
            task.setStatus("IN_PROGRESS");
            sendTaskUpdateToServer(task);
            sendDeveloperUpdate(task);
        }
        closeTaskWindow();
    }

    private void closeTaskWindow() {
        // Получаем текущее окно и закрываем его
        Stage stage = (Stage) taskButtonsVBox.getScene().getWindow();
        stage.close();
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

    private Boolean isThisUserInSystem(Long userId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/users/is_this_user/" + userId))
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

    private User fetchUserInfoFromServer(Long userId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/users/user_info?userId=" + userId))
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

    private Task fetchTaskInfoById(Long taskId) {
        if (taskId != null) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:8080/api/tasks/task_info/" + taskId))
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

    private void sendTaskUpdateToServer(Task task) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String taskJson = mapper.writeValueAsString(task);
            System.out.println("Отправляемый JSON: " + taskJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/tasks/" + task.getId()))
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

    private void sendDeveloperUpdate(Task task) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String taskJson = mapper.writeValueAsString(task);
            System.out.println("Отправляемый JSON: " + taskJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/tasks/" + task.getId() + "/set_developer"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", SessionManager.getAuthToken())
                    .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(task)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Задача успешно обновлена на сервере: поменяли разраба");
            } else {
                System.err.println("Ошибка при обновлении задачи: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendTesterUpdate(Task task) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String taskJson = mapper.writeValueAsString(task);
            System.out.println("Отправляемый JSON: " + taskJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/tasks/" + task.getId() + "/set_tester"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", SessionManager.getAuthToken())
                    .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(task)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Задача успешно обновлена на сервере: поменяли тестера");
            } else {
                System.err.println("Ошибка при обновлении задачи: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
