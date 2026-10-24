package com.zelora.app.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.zelora.app.R;

import java.util.ArrayList;
import java.util.List;

// ui
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.VH> {
    private final Context context;
    private List<CardRow> rows = new ArrayList<>();

    public CardAdapter(Context context) {
        this.context = context;
    }

    public void submit(List<CardRow> list) {
        rows = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context).inflate(R.layout.item_card_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CardRow r = rows.get(position);
        h.title.setText(r.title);
        show(h.subtitle, r.subtitle);
        show(h.body, r.body);
        show(h.badge, r.badge);
        show(h.value, r.value);

        h.buttons.removeAllViews();
        h.buttons.setVisibility(r.actions.isEmpty() ? View.GONE : View.VISIBLE);
        for (int i = 0; i < r.actions.size(); i++) {
            final CardRow.Action a = r.actions.get(i);
            TextView b = new TextView(context);
            b.setText(a.label);
            b.setTextSize(12.5f);
            b.setGravity(Gravity.CENTER);
            b.setSingleLine(true);
            b.setEllipsize(TextUtils.TruncateAt.END);
            b.setPadding(UiUtil.dp(context, 6), 0, UiUtil.dp(context, 6), 0);
            b.setClickable(true);
            b.setFocusable(true);
            b.setTypeface(ResourcesCompat.getFont(context, R.font.inter_semibold));
            b.setBackgroundResource(a.primary ? R.drawable.bg_btn_dark : R.drawable.bg_btn_outline);
            b.setTextColor(ContextCompat.getColor(context, a.primary ? R.color.white : R.color.brown_dark));
            b.setOnClickListener(v -> a.run.run());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiUtil.dp(context, 40), 1f);
            if (i > 0) lp.setMarginStart(UiUtil.dp(context, 8));
            h.buttons.addView(b, lp);
        }
    }

    private static void show(TextView tv, String text) {
        if (text == null || text.isEmpty()) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(text);
            tv.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title, subtitle, body, badge, value;
        final LinearLayout buttons;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tvTitle);
            subtitle = v.findViewById(R.id.tvSubtitle);
            body = v.findViewById(R.id.tvBody);
            badge = v.findViewById(R.id.tvBadge);
            value = v.findViewById(R.id.tvValue);
            buttons = v.findViewById(R.id.buttonRow);
        }
    }
}
