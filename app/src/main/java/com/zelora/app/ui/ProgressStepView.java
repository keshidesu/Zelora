package com.zelora.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.zelora.app.R;

// delivery
public class ProgressStepView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String[] labels = {"Accepted", "Picked up", "Delivered"};
    private boolean[] done = new boolean[3];
    private final int brown, ink;

    public ProgressStepView(Context c) { this(c, null); }

    public ProgressStepView(Context c, AttributeSet a) {
        super(c, a);
        brown = ContextCompat.getColor(c, R.color.btn_primary);
        ink = ContextCompat.getColor(c, R.color.text_dark);
    }

    public void setSteps(String[] labels, boolean[] done) {
        this.labels = labels;
        this.done = done;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), UiUtil.dp(getContext(), 64));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        int n = labels.length;
        float colW = getWidth() / (float) n, cy = 14 * d, r = 11 * d;

        p.setStrokeWidth(2 * d);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(brown);
        for (int i = 0; i < n - 1; i++) {
            canvas.drawLine(colW * (i + 0.5f) + r, cy, colW * (i + 1.5f) - r, cy, p);
        }
        for (int i = 0; i < n; i++) {
            float cx = colW * (i + 0.5f);
            if (done[i]) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(brown);
                canvas.drawCircle(cx, cy, r, p);
                p.setStyle(Paint.Style.STROKE);
                p.setColor(0xFFFFFFFF);
                p.setStrokeWidth(2 * d);
                p.setStrokeCap(Paint.Cap.ROUND);
                Path tick = new Path();
                tick.moveTo(cx - 4.5f * d, cy + 0.5f * d);
                tick.lineTo(cx - 1.2f * d, cy + 3.6f * d);
                tick.lineTo(cx + 4.8f * d, cy - 3.2f * d);
                canvas.drawPath(tick, p);
            } else {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFFFFFFFF);
                canvas.drawCircle(cx, cy, r, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2 * d);
                p.setColor(brown);
                canvas.drawCircle(cx, cy, r, p);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(ink);
            p.setTextSize(14 * d);
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(labels[i], cx, cy + r + 22 * d, p);
        }
    }
}
