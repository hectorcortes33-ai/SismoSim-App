package com.ejemplo.sismosim;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlynkApi {

    // URL base oficial de la API HTTP de Blynk IoT
    private static final String BASE_URL = "https://blynk.cloud/external/api/";

    // Ejecutor para peticiones en segundo plano (evita que la app se trabe)
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    // Handler para regresar la respuesta a la interfaz principal
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public interface BlynkCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    // Método para leer (Ej. V1 o V3)
    public static void readPin(String token, String pin, BlynkCallback callback) {
        String urlString = BASE_URL + "get?token=" + token + "&" + pin;
        executeRequest(urlString, callback);
    }

    // Método para escribir (Ej. V2)
    public static void writePin(String token, String pin, String value, BlynkCallback callback) {
        String urlString = BASE_URL + "update?token=" + token + "&" + pin + "=" + value;
        executeRequest(urlString, callback);
    }

    // Lógica interna para ejecutar la conexión
    private static void executeRequest(String urlString, BlynkCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Configuración de tiempos límite de espera (Timeouts)
                connection.setConnectTimeout(4000); // 4 segundos máximo para conectar
                connection.setReadTimeout(4000);    // 4 segundos máximo para leer datos

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Regresamos el éxito al hilo principal
                    handler.post(() -> callback.onSuccess(response.toString()));
                } else {
                    handler.post(() -> callback.onError("Error HTTP: " + responseCode));
                }
            } catch (Exception e) {
                handler.post(() -> callback.onError("Error de red: " + e.getMessage()));
            }
        });
    }
}