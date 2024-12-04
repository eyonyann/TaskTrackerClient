package org.example.tasktrackerclient.services;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AuthService {
    public static String register(String fullname, String username, String password, String role) {
        try {
            URL url = new URL("http://localhost:8080/api/register");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Тело запроса
            String jsonInput = String.format("{\"fullname\":\"%s\", \"username\":\"%s\", \"password\":\"%s\", \"role\":\"%s\"}",
                    fullname, username, password, role);

            // Запись тела запроса
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Чтение ответа
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // Здесь читайте токен из заголовка авторизации
                return conn.getHeaderField("Authorization");
            } else {
                System.out.println("Failed to register: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
