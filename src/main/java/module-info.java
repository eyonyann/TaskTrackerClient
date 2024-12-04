module org.example.tasktracker.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires static lombok;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;
    requires jsoup;

    opens org.example.tasktrackerclient.models to com.fasterxml.jackson.databind;
    opens org.example.tasktrackerclient.dtos to com.fasterxml.jackson.databind; // Добавлено
    opens org.example.tasktrackerclient.controllers to javafx.fxml;

    exports org.example.tasktrackerclient;
    exports org.example.tasktrackerclient.controllers;
}
