package org.example.tasktrackerclient.controllers;

import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.tasktrackerclient.SessionManager;
import org.example.tasktrackerclient.TaskTrackerClient;
import org.example.tasktrackerclient.services.AuthService;

import java.io.IOException;

public class SignUpController {

    @FXML
    public TextField fullnameField;

    @FXML
    private Hyperlink signUpLink;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private ChoiceBox<String> roleChoiceBox;

    @FXML
    private Button continueButton;

    @FXML
    private void signUp(ActionEvent event) throws IOException {
        String fullname = fullnameField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = roleChoiceBox.getValue();

        role = switch (role) {
            case "Разработчик" -> "DEVELOPER";
            case "Тестировщик" -> "TESTER";
            default -> role;
        };

        if (!password.equals(confirmPassword)) {
            // Вывод ошибки, если пароли не совпадают
            System.out.println("Passwords do not match");
            return;
        }

        // Вызов сервиса для регистрации
        String token = AuthService.register(fullname, username, password, role);
        if (token != null) {
            System.out.println("Registration successful, token: " + token);
            SessionManager.setAuthToken(token);

            FXMLLoader loader = new FXMLLoader(TaskTrackerClient.class.getResource("MainPage.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();

            ((Stage) continueButton.getScene().getWindow()).close();
        } else {
            System.out.println("Registration failed");
        }
    }

    @FXML
    private void openSignIn(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(TaskTrackerClient.class.getResource("SignIn.fxml"));
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

}
