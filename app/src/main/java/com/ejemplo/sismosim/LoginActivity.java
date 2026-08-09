package com.ejemplo.sismosim;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private ImageView imgLogo;
    private TextView txtTitulo, txtRegistrar;
    private EditText edtCorreo, edtPass;
    private Button btnEntrar;

    // Instancia para gestionar SQLite
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar el helper de base de datos
        dbHelper = new DatabaseHelper(this);

        // 1. Vincular componentes mediante sus IDs exactos
        imgLogo = findViewById(R.id.imgLogo);
        txtTitulo = findViewById(R.id.txtTitulo);
        edtCorreo = findViewById(R.id.edtCorreo);
        edtPass = findViewById(R.id.edtPass);
        btnEntrar = findViewById(R.id.btnEntrar);
        txtRegistrar = findViewById(R.id.txtRegistrar);

        // 2. Evento al pulsar el botón INGRESAR
        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validarEIngresar();
            }
        });

        // 3. Evento al pulsar en "Crear cuenta" (Navega a RegisterActivity)
        txtRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void validarEIngresar() {
        String correo = edtCorreo.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        // Validación 1: Campo Correo vacío
        if (correo.isEmpty()) {
            edtCorreo.setError("Ingresa tu correo");
            edtCorreo.requestFocus();
            return;
        }

        // Validación 2: Formato de correo
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            edtCorreo.setError("Ingresa un correo electrónico válido");
            edtCorreo.requestFocus();
            return;
        }

        // Validación 3: Campo Contraseña vacío
        if (pass.isEmpty()) {
            edtPass.setError("Ingresa tu contraseña");
            edtPass.requestFocus();
            return;
        }

        // --- CONSULTA EN BASE DE DATOS SQLITE ---
        // Valida las credenciales aplicando SHA-256 a la contraseña ingresada
        boolean esValido = dbHelper.validarUsuario(correo, pass);

        if (esValido) {
            Toast.makeText(this, "Acceso correcto", Toast.LENGTH_SHORT).show();

            // Redirección a la pantalla principal
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Cierra LoginActivity para evitar regresar con el botón atrás
        } else {
            Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            edtPass.setError("Credenciales inválidas");
            edtPass.requestFocus();
        }
    }
}