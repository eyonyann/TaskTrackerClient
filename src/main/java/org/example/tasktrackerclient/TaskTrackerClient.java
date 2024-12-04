package org.example.tasktrackerclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class TaskTrackerClient extends Application {
    @Override
    public void start(Stage stage) throws IOException {
//        System.out.println(getClass().getResource("/styles/SignIn.css"));
        FXMLLoader fxmlLoader = new FXMLLoader(TaskTrackerClient.class.getResource("SignIn.fxml"));
        //FXMLLoader fxmlLoader = new FXMLLoader(TaskTrackerClient.class.getResource("MainPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Task tracker");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}