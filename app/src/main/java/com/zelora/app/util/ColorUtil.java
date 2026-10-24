package com.zelora.app.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

import androidx.core.content.ContextCompat;

import com.zelora.app.R;

// color
public final class ColorUtil {
    private ColorUtil() {}

    public static int swatch(Context c, String name) {
        int res;
        switch (name.toLowerCase()) {
            case "red":    res = R.color.sw_red; break;
            case "blue":   res = R.color.sw_blue; break;
            case "brown":  res = R.color.sw_brown; break;
            case "yellow": res = R.color.sw_yellow; break;
            default:       res = R.color.sw_cream; break;
        }
        return ContextCompat.getColor(c, res);
    }

    public static LayerDrawable circle(Context c, String colorName, boolean selected) {
        float d = c.getResources().getDisplayMetrics().density;

        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(Color.TRANSPARENT);
        ring.setStroke(Math.round(1.5f * d),
                selected ? ContextCompat.getColor(c, R.color.brown_dark) : Color.TRANSPARENT);

        GradientDrawable fill = new GradientDrawable();
        fill.setShape(GradientDrawable.OVAL);
        fill.setColor(swatch(c, colorName));
        if (colorName.equalsIgnoreCase("cream")) {
            fill.setStroke(Math.round(1.2f * d), ContextCompat.getColor(c, R.color.sw_cream_stroke));
        }

        LayerDrawable ld = new LayerDrawable(new GradientDrawable[]{ring, fill});
        int inset = Math.round((selected ? 4 : 2) * d);
        ld.setLayerInset(1, inset, inset, inset, inset);
        return ld;
    }
}
