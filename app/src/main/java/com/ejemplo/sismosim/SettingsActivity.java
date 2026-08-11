package com.ejemplo.sismosim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText edtToken, edtIntervalo, edtUmbral;
    private TextView txtResultado;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Habilitar flecha de regreso en la barra superior
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ajustes");
        }

        // 1. Enlace de vistas
        edtToken = findViewById(R.id.edtToken);
        edtIntervalo = findViewById(R.id.edtIntervalo);
        edtUmbral = findViewById(R.id.edtUmbral);
        Button btnProbar = findViewById(R.id.btnProbar);
        txtResultado = findViewById(R.id.txtResultado);
        Button btnSalir = findViewById(R.id.btnSalir);

        sharedPreferences = getSharedPreferences("SismoPrefs", Context.MODE_PRIVATE);

        // Cargar ajustes guardados previamente
        cargarAjustes();

        // 2. Escuchadores con Lambdas
        if (btnProbar != null) {
            btnProbar.setOnClickListener(v -> probarConexion());
        }

        if (btnSalir != null) {
            btnSalir.setOnClickListener(v -> cerrarSesion());
        }
    }

    private void cargarAjustes() {
        String token = sharedPreferences.getString("blynk_token", "");
        int intervalo = sharedPreferences.getInt("intervalo", 2);
        float umbral = sharedPreferences.getFloat("umbral", 5.0f);

        edtToken.setText(token);
        edtIntervalo.setText(String.valueOf(intervalo));
        edtUmbral.setText(String.valueOf(umbral));
    }

    private void guardarAjustesAutomatico() {
        String token = edtToken.getText().toString().trim();
        String intervaloStr = edtIntervalo.getText().toString().trim();
        String umbralStr = edtUmbral.getText().toString().trim();

        int intervalo = 2;
        if (!intervaloStr.isEmpty()) {
            try {
                intervalo = Integer.parseInt(intervaloStr);
            } catch (NumberFormatException ignored) {}
        }

        float umbral = 5.0f;
        if (!umbralStr.isEmpty()) {
            try {
                umbral = Float.parseFloat(umbralStr);
            } catch (NumberFormatException ignored) {}
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("blynk_token", token);
        editor.putInt("intervalo", intervalo);
        editor.putFloat("umbral", umbral);
        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Guardado automático al salir de la pantalla
        guardarAjustesAutomatico();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void probarConexion() {
        guardarAjustesAutomatico();
        String token = edtToken.getText().toString().trim();
        String intervaloStr = edtIntervalo.getText().toString().trim();
        String umbralStr = edtUmbral.getText().toString().trim();

        if (token.isEmpty() || intervaloStr.isEmpty() || umbralStr.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();

            txtResultado.setText("Error: Campos incompletos");
            txtResultado.setTextColor(Color.parseColor("#D32F2F"));
            txtResultado.setVisibility(View.VISIBLE);
            return;
        }

        realizarPeticionBlynk(token);
    }

    private void realizarPeticionBlynk(final String token) {
        txtResultado.setText("Probando conexión...");
        txtResultado.setTextColor(Color.parseColor("#757575"));
        txtResultado.setVisibility(View.VISIBLE);

        BlynkApi.readPin(token, "v3", new BlynkApi.BlynkCallback() {
            @Override
            public void onSuccess(String response) {
                txtResultado.setText("Conexión correcta");
                txtResultado.setTextColor(Color.parseColor("#2E7D32"));
            }

            @Override
            public void onError(String error) {
                txtResultado.setText("Error de conexión");
                txtResultado.setTextColor(Color.parseColor("#D32F2F"));
            }
        });
    }

    private void cerrarSesion() {
        Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();

        sharedPreferences.edit().clear().apply();


        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}