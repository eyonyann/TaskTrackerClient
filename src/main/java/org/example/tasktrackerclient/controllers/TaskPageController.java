package org.example.tasktrackerclient.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
import org.example.tasktrackerclient.services.TaskService;
import org.example.tasktrackerclient.services.UserService;
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
import java.time.format.DateTimeFormatter;

public class TaskPageController {

    @FXML
    private HBox profileBox;

    @FXML
    public Label projectNameLabel;
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

    @FXML
    private VBox tableVBox;  // Основной контейнер для задач

    private VBox backlogVBox;
    private VBox inProgressVBox;
    private VBox reviewVBox;
    private VBox doneVBox;

    public void initialize() {
        initializeUserInfo();
        initializeProjectInfo();
        createTaskBoard();
        loadTasks();
    }

    private void createTaskBoard() {
        HBox taskBoard = new HBox(10);
        taskBoard.setPrefWidth(800);
        taskBoard.setPrefHeight(530);

        // Создаём и добавляем каждую колонку: BACKLOG, IN_PROGRESS, REVIEW, DONE
        taskBoard.getChildren().add(createTaskColumn("BACKLOG"));
        taskBoard.getChildren().add(createTaskColumn("IN_PROGRESS"));
        taskBoard.getChildren().add(createTaskColumn("REVIEW"));
        taskBoard.getChildren().add(createTaskColumn("DONE"));

        tableVBox.getChildren().add(taskBoard);
    }

    private VBox createTaskColumn(String title) {
        VBox column = new VBox();
        column.setPrefWidth(200);
        column.setStyle("-fx-background-color: #1b1b1b; -fx-border-color: gray; -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20;");

        // Заголовок колонки
        Label titleLabel = new Label(title);
        titleLabel.setMinSize(193, 55);
        titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #333; -fx-alignment: center; -fx-background-color: #afafaf; -fx-background-radius: 15;");
        VBox.setMargin(titleLabel, new Insets(0, 0, 10, 0));

        // Контейнер для задач
        VBox taskContainer = new VBox();
        taskContainer.setSpacing(15);
        taskContainer.setPrefHeight(130);
        taskContainer.setPadding(new Insets(10));

        // Добавляем заголовок и контейнер задач в колонку
        column.getChildren().addAll(titleLabel, taskContainer);

        // Делаем колонку доступной для дальнейшего использования в методе загрузки задач
        switch (title) {
            case "BACKLOG":
                backlogVBox = taskContainer;
                break;
            case "IN_PROGRESS":
                inProgressVBox = taskContainer;
                break;
            case "REVIEW":
                reviewVBox = taskContainer;
                break;
            case "DONE":
                doneVBox = taskContainer;
                break;
        }

        return column;
    }

    private void loadTasks() {
        Project project = fetchProjectInfoFromServer();
        if (project != null) {
            Long projectId = project.getId();
            List<Task> tasks = fetchTasksByProject(projectId);

            tasks.sort(Comparator.comparing(Task::getPriority));

            List<Task> backlogTasks = new ArrayList<>();
            List<Task> inProgressTasks = new ArrayList<>();
            List<Task> reviewTasks = new ArrayList<>();
            List<Task> doneTasks = new ArrayList<>();

            for (Task task : tasks) {
                switch (task.getStatus()) {
                    case "BACKLOG":
                        backlogTasks.add(task);
                        break;
                    case "IN_PROGRESS":
                        inProgressTasks.add(task);
                        break;
                    case "REVIEW":
                        reviewTasks.add(task);
                        break;
                    case "DONE":
                        doneTasks.add(task);
                        break;
                }
            }

            // Очистка текущих задач в колонках
            backlogVBox.getChildren().clear();
            inProgressVBox.getChildren().clear();
            reviewVBox.getChildren().clear();
            doneVBox.getChildren().clear();

            // Заполнение колонок задачами
            populateTaskColumn(backlogVBox, backlogTasks);
            populateTaskColumn(inProgressVBox, inProgressTasks);
            populateTaskColumn(reviewVBox, reviewTasks);
            populateTaskColumn(doneVBox, doneTasks);
        } else {
            System.err.println("Не удалось получить информацию о проекте");
        }
    }

    private void populateTaskColumn(VBox columnVBox, List<Task> tasks) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Task task : tasks) {
            VBox taskBox = new VBox();
            taskBox.setStyle("-fx-background-color: #afafaf; -fx-cursor: hand; -fx-padding: 10; -fx-border-color: lightgray; -fx-border-radius: 15; -fx-background-radius: 15;");

            // Название задачи
            Label nameLabel = new Label(task.getName());
            nameLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #333;");

            // Дедлайн задачи
            HBox deadlineBox = new HBox();
            Label deadlineLabel = new Label("Deadline:");
            deadlineLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #333;");
            Label deadlineValue = new Label(task.getDeadline().toLocalDate().format(dateFormatter));
            deadlineValue.setStyle("-fx-font-size: 14; -fx-text-fill: #333;");
            deadlineBox.getChildren().addAll(deadlineLabel, deadlineValue);

            // Приоритет задачи
            HBox priorityBox = new HBox();
            Label priorityLabel = new Label("Priority:");
            priorityLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #333;");
            Label priorityValue = new Label(task.getPriority().toString());
            priorityValue.setStyle("-fx-font-size: 14; -fx-text-fill: #333;");
            priorityBox.getChildren().addAll(priorityLabel, priorityValue);

            // Добавляем элементы в блок задачи
            taskBox.getChildren().addAll(nameLabel, deadlineBox, priorityBox);

            // Установка действия на клик по задаче

            taskBox.setOnMouseClicked(event -> openTaskWindow(task.getId()));

            // Добавляем задачу в колонку
            columnVBox.getChildren().add(taskBox);
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


    // Инициализация данных пользователя
    private void initializeUserInfo() {
        User user = fetchUserInfoFromServer();
        if (user != null) {
            profileNameLabel.setText(user.getFullname());
            profileRoleLabel.setText(user.getRole().name());
        } else {
            System.err.println("Не удалось загрузить данные пользователя");
        }
    }

    // Инициализация данных проекта
    private void initializeProjectInfo() {
        Project project = fetchProjectInfoFromServer();
        if (project != null) {
            projectNameLabel.setText(project.getName());
        } else {
            System.err.println("Не удалось загрузить данные проекта");
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



    private User fetchUserInfoFromServer(Long userId) {
        User user = null;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/users/user_info?userId=" + userId))
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