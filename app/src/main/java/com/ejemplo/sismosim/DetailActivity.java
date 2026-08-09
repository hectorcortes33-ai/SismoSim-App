package com.ejemplo.sismosim;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    private TextView txtTitulo, txtPico, txtDur, txtLecturas, txtOrigen;
    private GraficaView grafica;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // 1. Vincular componentes del XML
        txtTitulo = findViewById(R.id.txtTitulo);
        grafica = findViewById(R.id.grafica);
        txtPico = findViewById(R.id.txtPico);
        txtDur = findViewById(R.id.txtDur);
        txtLecturas = findViewById(R.id.txtLecturas);
        txtOrigen = findViewById(R.id.txtOrigen);
        Button btnCompartir = findViewById(R.id.btnCompartir);

        dbHelper = new DatabaseHelper(this);

        // 2. RECIBIR datos del Intent
        long eventoId = getIntent().getLongExtra("EVENTO_ID", -1);
        String horaEvento = getIntent().getStringExtra("hora_evento");

        List<Float> lecturas;

        // 3. CONSULTAR base de datos SQLite o usar respaldo de prueba
        if (eventoId != -1) {
            lecturas = obtenerLecturasDesdeBD(eventoId);
        } else {
            if (horaEvento == null || horaEvento.isEmpty()) {
                horaEvento = "10:42";
            }
            if (txtTitulo != null) {
                txtTitulo.setText(String.format("Evento %s", horaEvento));
            }
            lecturas = obtenerLecturasPrueba();
        }

        // 4. CALCULAR métricas y actualizar la gráfica Canvas con la ráfaga completa
        calcularYMostrarMetricas(lecturas);

        // 5. Configurar acción del botón Compartir
        if (btnCompartir != null) {
            btnCompartir.setOnClickListener(v -> compartirDetalles());
        }
    }

    private void calcularYMostrarMetricas(List<Float> lecturas) {
        if (lecturas == null || lecturas.isEmpty()) {
            if (txtPico != null) txtPico.setText("Pico: 0.00g");
            if (txtDur != null) txtDur.setText("Duración: 0s");
            if (txtLecturas != null) txtLecturas.setText("Lecturas: 0");
            return;
        }

        float maxPico = 0f;
        for (float val : lecturas) {
            if (Math.abs(val) > maxPico) {
                maxPico = Math.abs(val);
            }
        }

        int totalLecturas = lecturas.size();
        int duracionSegundos = totalLecturas;

        if (txtPico != null) txtPico.setText(String.format(Locale.US, "Pico: %.2fg", maxPico));
        if (txtDur != null) txtDur.setText(String.format(Locale.US, "Duración: %ds", duracionSegundos));
        if (txtLecturas != null) txtLecturas.setText(String.format(Locale.US, "Lecturas: %d", totalLecturas));

        // Pasa tanto el pico como los puntos de la ráfaga a GraficaView
        if (grafica != null) {
            grafica.setValorPico(maxPico);
            grafica.setLecturas(lecturas); // Envía los 15 puntos para renderizar la curva
        }
    }

    private List<Float> obtenerLecturasDesdeBD(long id) {
        List<Float> lecturas = new ArrayList<>();
        Cursor cursor = dbHelper.obtenerEventoPorId(id);

        if (cursor != null && cursor.moveToFirst()) {
            String fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENTO_FECHA));
            double pico = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENTO_PICO));
            String origen = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENTO_ORIGEN));

            if (txtTitulo != null) txtTitulo.setText(String.format("Evento %s", fecha));
            if (txtOrigen != null) txtOrigen.setText(String.format("Origen: %s", origen));

            float p = (float) pico;
            lecturas = Arrays.asList(
                    0.1f, 0.3f, p * 0.4f, -p * 0.7f, p, -p * 0.85f, p * 0.5f,
                    -p * 0.3f, p * 0.2f, -0.1f, 0.05f, 0.02f, 0.01f, 0.0f, 0.0f
            );
            cursor.close();
        } else {
            lecturas = obtenerLecturasPrueba();
        }
        return lecturas;
    }

    private List<Float> obtenerLecturasPrueba() {
        if (txtOrigen != null) txtOrigen.setText("Origen: Nodo TLP-01");
        return new ArrayList<>(Arrays.asList(
                0.2f, 0.5f, 1.1f, 2.3f, 4.18f, 3.2f, 2.1f, 1.5f, 0.9f, 0.6f, 0.4f, 0.3f, 0.2f, 0.1f, 0.05f
        ));
    }

    private void compartirDetalles() {
        String resumen = (txtTitulo != null ? txtTitulo.getText().toString() : "Evento Sísmico") + "\n" +
                (txtPico != null ? txtPico.getText().toString() : "") + "\n" +
                (txtDur != null ? txtDur.getText().toString() : "") + "\n" +
                (txtLecturas != null ? txtLecturas.getText().toString() : "") + "\n" +
                (txtOrigen != null ? txtOrigen.getText().toString() : "");

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, resumen);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Compartir evento sísmico");
        startActivity(shareIntent);
    }
}