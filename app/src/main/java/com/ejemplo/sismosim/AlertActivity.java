package com.ejemplo.sismosim;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlertActivity extends AppCompatActivity {

    private TextView txtAlerta, txtPico, txtOrigen, txtLeyenda;
    private Button btnCerrar;

    private static Ringtone ringtone;
    private static Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        // 1. Vincular componentes visuales
        txtAlerta = findViewById(R.id.txtAlerta);
        txtPico = findViewById(R.id.txtPico);
        txtOrigen = findViewById(R.id.txtOrigen);
        btnCerrar = findViewById(R.id.btnCerrar);
        txtLeyenda = findViewById(R.id.txtLeyenda);

        // 2. Recibir datos desde MainActivity
        double valorPico = getIntent().getDoubleExtra("VALOR_PICO", 0.0);
        String nodoNombre = getIntent().getStringExtra("NODO_NOMBRE");
        if (nodoNombre == null) nodoNombre = "Nodo TLP-01";

        String horaActual = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        // 3. Mostrar la información en pantalla
        if (txtPico != null) txtPico.setText(String.format(Locale.US, "%.2f g", valorPico));
        if (txtOrigen != null) txtOrigen.setText(nodoNombre + " · " + horaActual);

        // 4. Iniciar la reproducción del sonido de alarma y vibración
        iniciarAlertaSonoraYVibracion();

        // 5. Botón ENTENDIDO: Detiene el audio/vibración y destruye esta Activity
        if (btnCerrar != null) {
            btnCerrar.setOnClickListener(v -> {
                detenerAlerta();
                finish();
            });
        }
    }

    private void iniciarAlertaSonoraYVibracion() {
        try {
            detenerAlerta(); // Asegurarse de limpiar cualquier instancia anterior

            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (notification == null) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            if (ringtone != null) {
                ringtone.play();
            }

            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 500, 500};
                vibrator.vibrate(pattern, 0); // Repetir mientras la pantalla esté activa
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void detenerAlerta() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
            ringtone = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        detenerAlerta();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detenerAlerta();
    }
}