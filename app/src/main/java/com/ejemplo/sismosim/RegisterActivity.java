package com.ejemplo.sismosim;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private ImageView imgAvatar;
    private EditText edtNombre, edtCorreo, edtPass, edtPass2;
    private Button btnRegistrar;

    // Instancia para gestionar la base de datos SQLite
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar el helper de SQLite
        dbHelper = new DatabaseHelper(this);

        // 1. Vincular las vistas del layout XML
        imgAvatar = findViewById(R.id.imgAvatar);
        edtNombre = findViewById(R.id.edtNombre);
        edtCorreo = findViewById(R.id.edtCorreo);
        edtPass = findViewById(R.id.edtPass);
        edtPass2 = findViewById(R.id.edtPass2);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        // 2. Listener para procesar el registro
        btnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarUsuario();
            }
        });
    }

    private void registrarUsuario() {
        String nombre = edtNombre.getText().toString().trim();
        String correo = edtCorreo.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();
        String pass2 = edtPass2.getText().toString().trim();

        // Validación 1: Campo Nombre vacío
        if (nombre.isEmpty()) {
            edtNombre.setError("Ingresa tu nombre completo");
            edtNombre.requestFocus();
            return;
        }

        // Validación 2: Campo Correo vacío
        if (correo.isEmpty()) {
            edtCorreo.setError("Ingresa tu correo electrónico");
            edtCorreo.requestFocus();
            return;
        }

        // Validación 3: Formato de correo válido
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            edtCorreo.setError("Ingresa un correo electrónico válido");
            edtCorreo.requestFocus();
            return;
        }

        // Validación 4: Campo Contraseña vacío
        if (pass.isEmpty()) {
            edtPass.setError("Ingresa una contraseña");
            edtPass.requestFocus();
            return;
        }

        // Validación 5: Campo Confirmar Contraseña vacío
        if (pass2.isEmpty()) {
            edtPass2.setError("Confirma tu contraseña");
            edtPass2.requestFocus();
            return;
        }

        // Validación 6: Verificación de coincidencia de contraseñas
        if (!pass.equals(pass2)) {
            edtPass2.setError("Las contraseñas no coinciden");
            edtPass2.requestFocus();
            return;
        }

        // --- CONEXIÓN E INSERCIÓN EN LA BASE DE DATOS SQLITE ---
        // DatabaseHelper se encarga de aplicar el algoritmo SHA-256 a la contraseña
        boolean insertado = dbHelper.registrarUsuario(nombre, correo, pass);

        if (insertado) {
            Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();
            finish(); // Cierra la actividad y vuelve a LoginActivity
        } else {
            // Falla si el correo ya existe en la base de datos
            edtCorreo.setError("Este correo ya está registrado");
            edtCorreo.requestFocus();
            Toast.makeText(this, "No se pudo registrar. El correo ya existe.", Toast.LENGTH_LONG).show();
        }
    }
}