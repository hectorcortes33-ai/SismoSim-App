package com.ejemplo.sismosim;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

public class HistoryActivity extends AppCompatActivity {

    private ListView lstEventos;
    private DatabaseHelper dbHelper;
    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 1. Vincular vistas desde el XML
        TextView txtTitulo = findViewById(R.id.txtTitulo);
        lstEventos = findViewById(R.id.lstEventos);
        TextView txtVacio = findViewById(R.id.txtVacio);
        Button btnExportar = findViewById(R.id.btnExportar);
        Button btnBorrar = findViewById(R.id.btnBorrar);

        if (txtTitulo != null) {
            txtTitulo.setText("Historial de Eventos");
        }

        if (txtVacio != null && lstEventos != null) {
            lstEventos.setEmptyView(txtVacio);
        }

        dbHelper = new DatabaseHelper(this);

        // 2. Clic en un elemento para ver su detalle
        if (lstEventos != null) {
            lstEventos.setOnItemClickListener((parent, view, position, id) -> {
                Intent intent = new Intent(HistoryActivity.this, DetailActivity.class);
                intent.putExtra("EVENTO_ID", id);
                startActivity(intent);
            });
        }

        // 3. Listeners de botones
        if (btnExportar != null) {
            btnExportar.setOnClickListener(v -> exportarHistorialCSV());
        }

        if (btnBorrar != null) {
            btnBorrar.setOnClickListener(v -> confirmarBorrado());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarEventosDesdeBD();
    }

    private void cargarEventosDesdeBD() {
        Cursor nuevoCursor = dbHelper.obtenerEventos();

        if (adapter == null) {
            String[] fromColumns = {DatabaseHelper.COL_EVENTO_FECHA, DatabaseHelper.COL_EVENTO_PICO};
            int[] toViews = {android.R.id.text1, android.R.id.text2};

            adapter = new SimpleCursorAdapter(
                    this,
                    android.R.layout.simple_list_item_2,
                    nuevoCursor,
                    fromColumns,
                    toViews,
                    0
            );

            adapter.setViewBinder((view, cursor, columnIndex) -> {
                if (view.getId() == android.R.id.text2) {
                    double pico = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENTO_PICO));
                    String origen = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENTO_ORIGEN));
                    ((TextView) view).setText(String.format("Pico: %.2f g | Origen: %s", pico, origen));
                    return true;
                }
                return false;
            });

            if (lstEventos != null) {
                lstEventos.setAdapter(adapter);
            }
        } else {
            // Actualizar cursor de forma limpia en el adaptador existente
            adapter.changeCursor(nuevoCursor);
        }
    }

    private void confirmarBorrado() {
        new AlertDialog.Builder(this)
                .setTitle("Borrar Historial")
                .setMessage("¿Estás seguro de que deseas eliminar todos los eventos registrados?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    boolean exito = dbHelper.vaciarHistorial();
                    if (exito) {
                        cargarEventosDesdeBD();
                        Toast.makeText(HistoryActivity.this, "Historial eliminado con éxito", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(HistoryActivity.this, "Error al eliminar el historial", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void exportarHistorialCSV() {
        Cursor cursor = dbHelper.obtenerEventos();
        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "No hay eventos registrados para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvData = new StringBuilder();
        csvData.append("ID,Fecha_Hora,Pico_G,Duracion_S,Origen\n");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENTO_ID));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENTO_FECHA));
                double pico = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENTO_PICO));
                int duracion = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENTO_DURACION));
                String origen = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENTO_ORIGEN));

                csvData.append(id).append(",")
                        .append(fecha).append(",")
                        .append(pico).append(",")
                        .append(duracion).append(",")
                        .append(origen).append("\n");
            } while (cursor.moveToNext());
        }
        cursor.close();

        try {
            File file = new File(getCacheDir(), "historial_sismos.csv");
            FileOutputStream out = new FileOutputStream(file);
            out.write(csvData.toString().getBytes());
            out.close();

            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/csv");
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(sendIntent, "Exportar historial sísmico"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al generar el archivo CSV", Toast.LENGTH_SHORT).show();
        }
    }
}