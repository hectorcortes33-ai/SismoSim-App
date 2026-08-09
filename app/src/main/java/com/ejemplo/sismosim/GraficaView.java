package com.ejemplo.sismosim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class GraficaView extends View {

    private Paint bgPaint, linePaint, axisPaint, textPaint;
    private Path path;

    // Arreglo con las 15 lecturas por defecto
    private float[] lecturas15 = new float[]{0.1f, 0.2f, 0.15f, 0.8f, -1.5f, 4.18f, -3.2f, 1.9f, -0.9f, 0.5f, -0.3f, 0.2f, 0.1f, 0.05f, 0.0f};

    public GraficaView(Context context) {
        super(context);
        init();
    }

    public GraficaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GraficaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Fondo gris claro
        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#E0E0E0"));

        // Eje cero central
        axisPaint = new Paint();
        axisPaint.setColor(Color.parseColor("#B0BEC5"));
        axisPaint.setStrokeWidth(2f);

        // Línea para trazar la curva del sismo (Rojo sísmico)
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#D32F2F"));
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        // Texto descriptivo
        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#555555"));
        textPaint.setTextSize(26f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        path = new Path();
    }

    /**
     * Recibe la lista de lecturas en tiempo real o desde SQLite y actualiza el Canvas
     */
    public void setLecturas(List<Float> lecturas) {
        if (lecturas != null && !lecturas.isEmpty()) {
            this.lecturas15 = new float[lecturas.size()];
            for (int i = 0; i < lecturas.size(); i++) {
                this.lecturas15[i] = lecturas.get(i);
            }
            invalidate(); // Redibuja el Canvas con los nuevos puntos
        }
    }

    /**
     * Genera la envolvente amortiguada de 15 lecturas basada en la aceleración pico recibida
     */
    public void setValorPico(double pico) {
        float p = (float) pico;
        this.lecturas15 = new float[]{
                0.05f, 0.1f, 0.3f, p * 0.4f, -p * 0.7f, p, -p * 0.85f, p * 0.5f, -p * 0.3f, p * 0.2f, -0.1f, 0.05f, 0.02f, 0.0f, 0.0f
        };
        invalidate(); // Redibuja el Canvas con los nuevos valores
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float centerY = h / 2f;

        // 1. Dibujar el recuadro de fondo
        canvas.drawRect(0, 0, w, h, bgPaint);

        // 2. Dibujar eje neutro cero
        canvas.drawLine(0, centerY, w, centerY, axisPaint);

        // 3. Trazar la curva de las lecturas con Path
        path.reset();
        if (lecturas15 != null && lecturas15.length > 0) {
            float pasoX = (float) w / (lecturas15.length - 1);

            float maxG = 1.0f;
            for (float val : lecturas15) {
                if (Math.abs(val) > maxG) maxG = Math.abs(val);
            }

            float maxAmp = centerY * 0.75f; // Usar el 75% de la altura semi-vertical

            for (int i = 0; i < lecturas15.length; i++) {
                float x = i * pasoX;
                float y = centerY - (lecturas15[i] / maxG) * maxAmp;

                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
        }

        canvas.drawPath(path, linePaint);

        // 4. Dibujar texto indicativo
        canvas.drawText("Curva de las 15 lecturas", w / 2f, 35f, textPaint);
    }
}