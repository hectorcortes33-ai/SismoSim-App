package com.ejemplo.sismosim;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREF_NAME = "sismosim_prefs";

    // Claves de almacenamiento
    private static final String KEY_BLYNK_TOKEN = "blynk_token";
    private static final String KEY_UMBRAL = "umbral_alerta";
    private static final String KEY_INTERVALO = "intervalo_sondeo";
    private static final String KEY_USUARIO_SESION = "usuario_sesion";

    // Valores por defecto
    private static final String DEFAULT_TOKEN = "O0ka5SZ0uenE8NriWZ0GXI-4xpkbYfXw";
    private static final float DEFAULT_UMBRAL = 3.0f; // 3.0 g
    private static final int DEFAULT_INTERVALO = 1000; // 1000 ms (1s)

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // --- TOKEN DE BLYNK ---
    public void setBlynkToken(String token) {
        prefs.edit().putString(KEY_BLYNK_TOKEN, token).apply();
    }

    public String getBlynkToken() {
        return prefs.getString(KEY_BLYNK_TOKEN, DEFAULT_TOKEN);
    }

    // --- UMBRAL DE ALERTA (en g) ---
    public void setUmbralAlerta(float umbral) {
        prefs.edit().putFloat(KEY_UMBRAL, umbral).apply();
    }

    public float getUmbralAlerta() {
        return prefs.getFloat(KEY_UMBRAL, DEFAULT_UMBRAL);
    }

    // --- INTERVALO DE SONDEO (en milisegundos) ---
    public void setIntervaloSondeo(int ms) {
        prefs.edit().putInt(KEY_INTERVALO, ms).apply();
    }

    public int getIntervaloSondeo() {
        return prefs.getInt(KEY_INTERVALO, DEFAULT_INTERVALO);
    }

    // --- USUARIO DE LA SESIÓN ACTUAL ---
    public void setUsuarioSesion(String correo) {
        prefs.edit().putString(KEY_USUARIO_SESION, correo).apply();
    }

    public String getUsuarioSesion() {
        return prefs.getString(KEY_USUARIO_SESION, "invitado@sismosim.com");
    }
}