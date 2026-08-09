package com.ejemplo.sismosim;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Habilitar la flecha de regreso en la Action Bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Acerca de");
        }

        // 1. Vincular los 4 TextViews definidos en el XML
        TextView txtTitulo = findViewById(R.id.txtTitulo);
        TextView txtEquipo = findViewById(R.id.txtEquipo);
        TextView txtVersion = findViewById(R.id.txtVersion);
        TextView txtLeyenda = findViewById(R.id.txtLeyenda);

        // 2. Obtener la versión de la app de forma segura
        String versionName = "1.0"; // Valor por defecto
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // 3. Asignar la versión leída dinámicamente
        if (txtVersion != null) {
            txtVersion.setText(String.format(Locale.getDefault(), "Versión %s · agosto 2026", versionName));
        }
    }

    // Acción para que la flecha de regreso apague la pantalla y vuelva a la anterior
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}