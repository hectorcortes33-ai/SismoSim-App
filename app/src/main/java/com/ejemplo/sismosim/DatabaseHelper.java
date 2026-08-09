package com.ejemplo.sismosim;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sismosim.db";
    private static final int DATABASE_VERSION = 1;

    // --- TABLA USUARIOS ---
    public static final String TABLA_USUARIOS = "usuarios";
    public static final String COL_USUARIO_ID = "_id";
    public static final String COL_USUARIO_NOMBRE = "nombre";
    public static final String COL_USUARIO_CORREO = "correo";
    public static final String COL_USUARIO_PASS = "password";

    // --- TABLA EVENTOS ---
    public static final String TABLA_EVENTOS = "eventos";
    public static final String COL_EVENTO_ID = "_id";
    public static final String COL_EVENTO_FECHA = "fecha_hora";
    public static final String COL_EVENTO_PICO = "pico";
    public static final String COL_EVENTO_DURACION = "duracion";
    public static final String COL_EVENTO_ORIGEN = "origen";

    // --- TABLA LECTURAS (15 puntos por evento) ---
    public static final String TABLA_LECTURAS = "lecturas";
    public static final String COL_LECTURA_ID = "_id";
    public static final String COL_LECTURA_EVENTO_ID = "evento_id";
    public static final String COL_LECTURA_SEGUNDO = "segundo";
    public static final String COL_LECTURA_VALOR = "valor_g";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Crear tabla usuarios
        String createUsuarios = "CREATE TABLE " + TABLA_USUARIOS + " (" +
                COL_USUARIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USUARIO_NOMBRE + " TEXT, " +
                COL_USUARIO_CORREO + " TEXT UNIQUE, " +
                COL_USUARIO_PASS + " TEXT)";
        db.execSQL(createUsuarios);

        // 2. Crear tabla eventos
        String createEventos = "CREATE TABLE " + TABLA_EVENTOS + " (" +
                COL_EVENTO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EVENTO_FECHA + " TEXT, " +
                COL_EVENTO_PICO + " REAL, " +
                COL_EVENTO_DURACION + " INTEGER, " +
                COL_EVENTO_ORIGEN + " TEXT)";
        db.execSQL(createEventos);

        // 3. Crear tabla lecturas de 15 puntos por sismo
        String createLecturas = "CREATE TABLE " + TABLA_LECTURAS + " (" +
                COL_LECTURA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LECTURA_EVENTO_ID + " INTEGER, " +
                COL_LECTURA_SEGUNDO + " INTEGER, " +
                COL_LECTURA_VALOR + " REAL, " +
                "FOREIGN KEY(" + COL_LECTURA_EVENTO_ID + ") REFERENCES " + TABLA_EVENTOS + "(" + COL_EVENTO_ID + "))";
        db.execSQL(createLecturas);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLA_USUARIOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLA_EVENTOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLA_LECTURAS);
        onCreate(db);
    }

    // ==========================================
    // SECCIÓN 1: AUTENTICACIÓN (CON HASH SHA-256)
    // ==========================================

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean registrarUsuario(String nombre, String correo, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USUARIO_NOMBRE, nombre);
        values.put(COL_USUARIO_CORREO, correo);
        values.put(COL_USUARIO_PASS, hashPassword(password));

        long resultado = db.insert(TABLA_USUARIOS, null, values);
        return resultado != -1;
    }

    public boolean validarUsuario(String correo, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String passHash = hashPassword(password);

        String[] columns = {COL_USUARIO_ID};
        String selection = COL_USUARIO_CORREO + " = ? AND " + COL_USUARIO_PASS + " = ?";
        String[] selectionArgs = {correo, passHash};

        Cursor cursor = db.query(TABLA_USUARIOS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();

        return count > 0;
    }

    // ==========================================
    // SECCIÓN 2: REGISTRO DE EVENTOS SÍSMICOS
    // ==========================================

    public long guardarEventoCompleto(double picoMaximo, String origen, List<Double> lecturas15Segundos) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long eventoId = -1;

        try {
            // Formateador con la zona horaria de México/América Central
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
            String fechaActual = sdf.format(new Date());

            ContentValues valuesEvento = new ContentValues();
            valuesEvento.put(COL_EVENTO_FECHA, fechaActual);
            valuesEvento.put(COL_EVENTO_PICO, picoMaximo);
            valuesEvento.put(COL_EVENTO_DURACION, 15);
            valuesEvento.put(COL_EVENTO_ORIGEN, origen);

            eventoId = db.insert(TABLA_EVENTOS, null, valuesEvento);

            if (eventoId != -1 && lecturas15Segundos != null) {
                for (int i = 0; i < lecturas15Segundos.size(); i++) {
                    ContentValues valuesLectura = new ContentValues();
                    valuesLectura.put(COL_LECTURA_EVENTO_ID, eventoId);
                    valuesLectura.put(COL_LECTURA_SEGUNDO, i + 1);
                    valuesLectura.put(COL_LECTURA_VALOR, lecturas15Segundos.get(i));
                    db.insert(TABLA_LECTURAS, null, valuesLectura);
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return eventoId;
    }

    public Cursor obtenerEventos() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLA_EVENTOS + " ORDER BY " + COL_EVENTO_ID + " DESC", null);
    }

    public Cursor obtenerEventoPorId(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLA_EVENTOS,
                null,
                COL_EVENTO_ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );
    }

    public List<Double> obtenerLecturasDeEvento(long eventoId) {
        List<Double> puntos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " + COL_LECTURA_VALOR + " FROM " + TABLA_LECTURAS +
                " WHERE " + COL_LECTURA_EVENTO_ID + " = ? ORDER BY " + COL_LECTURA_SEGUNDO + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(eventoId)});
        if (cursor.moveToFirst()) {
            do {
                puntos.add(cursor.getDouble(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return puntos;
    }

    // ==========================================
    // SECCIÓN 3: ELIMINACIÓN DE HISTORIAL
    // ==========================================

    public boolean vaciarHistorial() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Eliminar lecturas y luego los eventos
            db.delete(TABLA_LECTURAS, null, null);
            db.delete(TABLA_EVENTOS, null, null);
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
        }
    }
}