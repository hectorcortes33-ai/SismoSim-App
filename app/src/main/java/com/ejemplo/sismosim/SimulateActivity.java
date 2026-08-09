package com.ejemplo.sismosim;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SimulateActivity extends AppCompatActivity {

    private TextView txtMagnitud, txtEstadoEnvio;
    private Button btnDisparar;
    private ProgressBar prgEvento;
    private CountDownTimer timerSimulacion;

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private double magnitudSeleccionada = 4.2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulate);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Provocar Evento");
        }

        SeekBar skbMagnitud = findViewById(R.id.skbMagnitud);
        txtMagnitud = findViewById(R.id.txtMagnitud);
        btnDisparar = findViewById(R.id.btnDisparar);
        prgEvento = findViewById(R.id.prgEvento);
        txtEstadoEnvio = findViewById(R.id.txtEstadoEnvio);

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SismoPrefs", Context.MODE_PRIVATE);

        // Ocultar la barra inicialmente y definir su máximo en 15s
        if (prgEvento != null) {
            prgEvento.setVisibility(View.GONE);
            prgEvento.setMax(15);
        }

        if (txtMagnitud != null) {
            txtMagnitud.setText(String.format(Locale.US, "%.1f", magnitudSeleccionada));
        }

        if (skbMagnitud != null) {
            skbMagnitud.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    magnitudSeleccionada = progress / 10.0;
                    if (txtMagnitud != null) {
                        txtMagnitud.setText(String.format(Locale.US, "%.1f", magnitudSeleccionada));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (btnDisparar != null) {
            btnDisparar.setOnClickListener(v -> prepararEIniciarSimulacion());
        }
    }

    private void prepararEIniciarSimulacion() {
        String token = sharedPreferences.getString("blynk_token", "");

        // 1. Bloquear botón inmediatamente (Requisito estricto)
        btnDisparar.setEnabled(false);

        if (prgEvento != null) {
            prgEvento.setVisibility(View.VISIBLE);
            prgEvento.setProgress(0);
        }

        if (txtEstadoEnvio != null) {
            txtEstadoEnvio.setText("Escribiendo V2 = 1...");
            txtEstadoEnvio.setTextColor(Color.parseColor("#757575"));
        }

        // 2. Transmitir V2 = 1 a Blynk AL INICIO del sismo
        if (!token.isEmpty()) {
            BlynkApi.writePin(token, "v2", "1", new BlynkApi.BlynkCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> iniciarTemporizador15s(token));
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(SimulateActivity.this, "Error de red, simulando en modo local", Toast.LENGTH_SHORT).show();
                        iniciarTemporizador15s(token);
                    });
                }
            });
        } else {
            Toast.makeText(this, "Modo sin token: Simulando localmente", Toast.LENGTH_SHORT).show();
            iniciarTemporizador15s("");
        }
    }

    private void iniciarTemporizador15s(String token) {
        if (timerSimulacion != null) {
            timerSimulacion.cancel();
        }

        // Temporizador de 15 segundos
        timerSimulacion = new CountDownTimer(15000, 1000) {
            int segundos = 0;

            @Override
            public void onTick(long millisUntilFinished) {
                segundos++;
                if (prgEvento != null) {
                    prgEvento.setProgress(segundos);
                }
                if (txtEstadoEnvio != null) {
                    txtEstadoEnvio.setText(String.format(Locale.US, "Simulando evento sísmico... %d / 15 s", segundos));
                    txtEstadoEnvio.setTextColor(Color.parseColor("#F57C00")); // Naranja
                }
            }

            @Override
            public void onFinish() {
                if (prgEvento != null) {
                    prgEvento.setProgress(15);
                }
                finalizarYGuardarEvento(token);
            }
        }.start();
    }

    private void finalizarYGuardarEvento(String token) {
        String nodoId = sharedPreferences.getString("nodo_id", "Nodo TLP-01");

        // Guardar en SQLite
        List<Double> lecturas = generar15Lecturas(magnitudSeleccionada);
        long idRegistrado = dbHelper.guardarEventoCompleto(magnitudSeleccionada, nodoId, lecturas);

        // Restablecer V2 = 0 en Blynk al terminar el evento
        if (!token.isEmpty()) {
            BlynkApi.writePin(token, "v2", "0", new BlynkApi.BlynkCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> concluirUI(idRegistrado));
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> concluirUI(idRegistrado));
                }
            });
        } else {
            concluirUI(idRegistrado);
        }
    }

    private void concluirUI(long idRegistrado) {
        if (txtEstadoEnvio != null) {
            txtEstadoEnvio.setText(String.format(Locale.US, "Evento #%d registrado en SQLite (%.2f g)", idRegistrado, magnitudSeleccionada));
            txtEstadoEnvio.setTextColor(Color.parseColor("#2E7D32")); // Verde
        }
        btnDisparar.setEnabled(true); // Desbloquear botón
    }

    private List<Double> generar15Lecturas(double pico) {
        List<Double> lista = new ArrayList<>();
        double[] factor = {0.1, 0.3, 0.6, 0.9, 1.0, 0.8, 0.6, 0.4, 0.3, 0.2, 0.15, 0.1, 0.08, 0.05, 0.02};
        for (double f : factor) {
            double val = Math.round((pico * f) * 100.0) / 100.0;
            lista.add(val);
        }
        return lista;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerSimulacion != null) {
            timerSimulacion.cancel();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}