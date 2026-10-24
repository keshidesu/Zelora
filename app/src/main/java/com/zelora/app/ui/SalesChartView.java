package com.zelora.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.zelora.app.R;

import java.util.Calendar;
import java.util.Locale;

// chart
public class SalesChartView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] values = new int[7];
    private final String[] days = new String[7];

    public SalesChartView(Context c) { this(c, null); }

    public SalesChartView(Context c, AttributeSet attrs) {
        super(c, attrs);
        float d = getResources().getDisplayMetrics().density;
        int orange = ContextCompat.getColor(c, R.color.sale_text);
        line.setColor(orange);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(2f * d);
        line.setStrokeJoin(Paint.Join.ROUND);
        dot.setColor(orange);
        grid.setColor(ContextCompat.getColor(c, R.color.divider));
        grid.setStrokeWidth(1f * d);
        label.setColor(ContextCompat.getColor(c, R.color.text_dark));
        label.setTextSize(11f * d);
        label.setTextAlign(Paint.Align.CENTER);

        String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        Calendar cal = Calendar.getInstance(Locale.US);
        for (int i = 6; i >= 0; i--) {
            days[i] = names[cal.get(Calendar.DAY_OF_WEEK) - 1];
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
    }

    public void setValues(int[] v) {
        values = v;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float w = getWidth(), h = getHeight();
        float padX = 14 * d, top = 12 * d, bottom = h - 22 * d;
        int max = 1;
        for (int x : values) max = Math.max(max, x);

        canvas.drawLine(0, bottom, w, bottom, grid);

        int n = values.length;
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = padX + (w - 2 * padX) * i / (n - 1);
            ys[i] = bottom - (bottom - top) * values[i] / (float) max * 0.9f;
        }
        Path p = new Path();
        p.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) p.lineTo(xs[i], ys[i]);

        Path area = new Path(p);
        area.lineTo(xs[n - 1], bottom);
        area.lineTo(xs[0], bottom);
        area.close();
        fill.setShader(new LinearGradient(0, top, 0, bottom, 0x40E4A853, 0x05E4A853, Shader.TileMode.CLAMP));
        canvas.drawPath(area, fill);
        canvas.drawPath(p, line);
        for (int i = 0; i < n; i++) {
            canvas.drawCircle(xs[i], ys[i], 3 * d, dot);
            canvas.drawText(days[i], xs[i], h - 5 * d, label);
        }
    }
}
