package com.ejemplo.sismosim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView txtNodo, txtEstado, txtValor, txtLatido, txtLeyenda;
    private ProgressBar prgSondeo;
    private Button btnSimular;

    private final Handler handlerSondeo = new Handler(Looper.getMainLooper());
    private Runnable runnableSondeo;

    private String token = "";
    private int intervaloSegundos = 2;
    private float umbralAlerta = 5.0f;
    private boolean alertaDisparada = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Vincular componentes de la vista
        txtNodo = findViewById(R.id.txtNodo);
        txtEstado = findViewById(R.id.txtEstado);
        txtValor = findViewById(R.id.txtValor);
        txtLatido = findViewById(R.id.txtLatido);
        prgSondeo = findViewById(R.id.prgSondeo);
        btnSimular = findViewById(R.id.btnSimular);
        txtLeyenda = findViewById(R.id.txtLeyenda);

        // 2. Estado inicial visible de inmediato
        limpiarPantallaInicial();

        // 3. Navegación a la pantalla de Simular Sismo
        if (btnSimular != null) {
            btnSimular.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SimulateActivity.class);
                startActivity(intent);
            });
        }

        // 4. Atajo a Historial (al tocar el título del nodo)
        if (txtNodo != null) {
            txtNodo.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            });
        }

        // 5. Atajo a Ajustes (al tocar el texto de estado o latido)
        View.OnClickListener abrirAjustesListener = v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        };

        if (txtLatido != null) txtLatido.setOnClickListener(abrirAjustesListener);
        if (txtEstado != null) txtEstado.setOnClickListener(abrirAjustesListener);

        // 6. Bucle de sondeo
        runnableSondeo = new Runnable() {
            @Override
            public void run() {
                ejecutarSondeo();
                handlerSondeo.postDelayed(this, Math.max(intervaloSegundos, 2) * 1000L);
            }
        };
    }

    private void limpiarPantallaInicial() {
        if (txtEstado != null) {
            txtEstado.setText("en reposo");
            txtEstado.setTextColor(Color.parseColor("#00796B"));
        }
        if (txtValor != null) txtValor.setText("0.00 g");
        if (txtLatido != null) {
            txtLatido.setText("Sondeo activo cada " + intervaloSegundos + " s");
            txtLatido.setTextColor(Color.parseColor("#616161"));
        }
        if (prgSondeo != null) prgSondeo.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarConfiguracion();

        handlerSondeo.removeCallbacks(runnableSondeo);
        handlerSondeo.post(runnableSondeo);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handlerSondeo.removeCallbacks(runnableSondeo);
    }

    private void cargarConfiguracion() {
        SharedPreferences prefs = getSharedPreferences("SismoPrefs", Context.MODE_PRIVATE);
        String nombreNodo = prefs.getString("nodo_id", "Nodo TLP-01");
        token = prefs.getString("blynk_token", "");
        intervaloSegundos = prefs.getInt("intervalo", 2);
        umbralAlerta = prefs.getFloat("umbral", 5.0f);

        if (txtNodo != null) txtNodo.setText(nombreNodo);
        if (txtLatido != null && (token != null && !token.trim().isEmpty())) {
            txtLatido.setText("Sondeo activo cada " + intervaloSegundos + " s");
        }
    }

    private void ejecutarSondeo() {
        // Sin Token configurado
        if (token == null || token.trim().isEmpty()) {
            if (prgSondeo != null) prgSondeo.setVisibility(View.GONE);
            if (txtEstado != null) {
                txtEstado.setText("Sin Token");
                txtEstado.setTextColor(Color.parseColor("#D32F2F"));
            }
            if (txtValor != null) txtValor.setText("0.00 g");
            if (txtLatido != null) {
                txtLatido.setText("Agregar token (Toca aquí para configurar)");
                txtLatido.setTextColor(Color.parseColor("#D32F2F"));
            }
            return;
        }

        // Con Token configurado
        if (txtLatido != null) {
            txtLatido.setTextColor(Color.parseColor("#616161"));
            txtLatido.setText("Sondeo activo cada " + intervaloSegundos + " s");
        }
        if (prgSondeo != null) prgSondeo.setVisibility(View.VISIBLE);

        // Leer V1 y V3 desde Blynk
        BlynkApi.readPin(token, "v1", new BlynkApi.BlynkCallback() {
            @Override
            public void onSuccess(String responseV1) {
                double valorV1 = parsearRespuestaDouble(responseV1);

                BlynkApi.readPin(token, "v3", new BlynkApi.BlynkCallback() {
                    @Override
                    public void onSuccess(String responseV3) {
                        runOnUiThread(() -> {
                            if (prgSondeo != null) prgSondeo.setVisibility(View.GONE);
                            String estadoV3 = parsearRespuestaTexto(responseV3);
                            actualizarInterfaz(valorV1, estadoV3);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            if (prgSondeo != null) prgSondeo.setVisibility(View.GONE);
                            actualizarInterfaz(valorV1, "error");
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (prgSondeo != null) prgSondeo.setVisibility(View.GONE);
                    actualizarInterfaz(0.0, "error");
                });
            }
        });
    }

    private double parsearRespuestaDouble(String raw) {
        try {
            String clean = raw.replace("\"", "").replace("[", "").replace("]", "").trim();
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String parsearRespuestaTexto(String raw) {
        if (raw == null) return "reposo";
        return raw.replace("\"", "").replace("[", "").replace("]", "").trim();
    }

    private void actualizarInterfaz(double valorG, String estado) {
        if (txtValor != null) txtValor.setText(String.format(Locale.US, "%.2f g", valorG));
        if (txtLatido != null) txtLatido.setText("Sondeo activo cada " + intervaloSegundos + " s");

        if (txtEstado == null) return;

        if ("error".equals(estado)) {
            txtEstado.setText("Sin Conexión");
            txtEstado.setTextColor(Color.GRAY);
            return;
        }

        boolean esEvento = "evento".equalsIgnoreCase(estado);
        boolean superaUmbral = valorG >= umbralAlerta;

        if (superaUmbral || esEvento) {
            if (superaUmbral) {
                txtEstado.setText("en alerta");
                txtEstado.setTextColor(Color.parseColor("#D32F2F"));

                // Solo abre la alerta 1 SOLA VEZ por cada sismo
                if (!alertaDisparada) {
                    alertaDisparada = true;
                    Intent intentAlerta = new Intent(MainActivity.this, AlertActivity.class);
                    intentAlerta.putExtra("VALOR_PICO", valorG);
                    intentAlerta.putExtra("NODO_NOMBRE", txtNodo.getText().toString());
                    startActivity(intentAlerta);
                }
            } else {
                txtEstado.setText("en evento");
                txtEstado.setTextColor(Color.parseColor("#F57C00"));
            }
        } else {
            // Cuando la magnitud baja del umbral (el sismo terminó), se rearma la alerta para el siguiente evento
            alertaDisparada = false;
            txtEstado.setText("en reposo");
            txtEstado.setTextColor(Color.parseColor("#00796B"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_simular) {
            startActivity(new Intent(this, SimulateActivity.class));
            return true;
        } else if (id == R.id.nav_historial) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        } else if (id == R.id.nav_ajustes) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.nav_acerca_de) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}