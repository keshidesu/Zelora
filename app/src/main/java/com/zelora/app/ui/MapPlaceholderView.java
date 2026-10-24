package com.zelora.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

// delivery
public class MapPlaceholderView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public MapPlaceholderView(Context c) { this(c, null); }
    public MapPlaceholderView(Context c, AttributeSet a) { super(c, a); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float w = getWidth(), h = getHeight();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFE9E1D6);
        canvas.drawRect(0, 0, w, h, paint);

        paint.setColor(0xFFDCE3C6);
        canvas.drawRoundRect(0, h * 0.10f, w * 0.30f, h * 0.38f, 10 * d, 10 * d, paint);
        canvas.drawRoundRect(w * 0.08f, h * 0.70f, w * 0.40f, h * 0.98f, 10 * d, 10 * d, paint);

        paint.setColor(0xFFF2ECE3);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                float x = w * (0.34f + c * 0.17f), y = h * (0.04f + r * 0.24f);
                canvas.drawRoundRect(x, y, x + w * 0.14f, y + h * 0.19f, 6 * d, 6 * d, paint);
            }
        }

        float px = w * 0.28f, py = h * 0.80f, dx = w * 0.82f, dy = h * 0.18f;
        Path route = new Path();
        route.moveTo(px, py);
        route.lineTo(px + w * 0.08f, h * 0.60f);
        route.lineTo(w * 0.42f, h * 0.60f);
        route.lineTo(w * 0.52f, h * 0.38f);
        route.lineTo(dx, h * 0.38f);
        route.lineTo(dx, dy);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5 * d);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(0xFF7A4A1E);
        canvas.drawPath(route, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF7A4A1E);
        canvas.drawCircle(px, py, 20 * d, paint);
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(px, py, 8 * d, paint);
        paint.setColor(0xFFD2691E);
        canvas.drawCircle(dx, dy, 14 * d, paint);
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(dx, dy, 5 * d, paint);
    }
}
