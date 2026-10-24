package com.zelora.app.ui;

import android.content.Context;
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
import com.zelora.app.model.Order;
import com.zelora.app.util.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// order
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

    public interface Listener {
        void onCancelOrder(Order order);
        void onRateOrder(Order order);
        void onReportProblem(Order order);
    }

    private final Context context;
    private final Listener listener;
    private List<Order> orders = new ArrayList<>();

    public OrderAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void submit(List<Order> list) {
        orders = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context).inflate(R.layout.item_order, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Order o = orders.get(position);
        h.code.setText(o.code);
        h.status.setText(o.status.toUpperCase(Locale.US));
        h.date.setText(UiUtil.formatDate(o.createdAt));
        h.shipTo.setText("Ship to: " + o.recipient + ", " + o.address);
        String payStatus;
        if ("PAID".equals(o.paymentStatus)) payStatus = "Paid";
        else if ("REFUNDED".equals(o.paymentStatus)) payStatus = "Refunded";
        else if ("CANCELLED".equals(o.paymentStatus)) payStatus = "Cancelled";
        else payStatus = "Pay on delivery";
        h.payment.setText("Payment: " + o.paymentMethod + " • " + payStatus
                + (o.shipping == 0 ? " • Free shipping" : " • Shipping " + Money.peso(o.shipping))
                + (o.discount > 0 ? " • Voucher " + o.voucherCode + " (-" + Money.peso(o.discount) + ")" : ""));

        StringBuilder extra = new StringBuilder();
        if (o.riderName != null && !Order.CANCELLED.equals(o.status)) extra.append("Rider: ").append(o.riderName);
        if (o.disputeStatus != null) {
            if (extra.length() > 0) extra.append('\n');
            extra.append("Your report (").append(o.disputeStatus).append(")");
            if (o.disputeNote != null && !o.disputeNote.isEmpty()) extra.append(": ").append(o.disputeNote);
        }
        h.extra.setText(extra.toString());
        h.extra.setVisibility(extra.length() == 0 ? View.GONE : View.VISIBLE);
        h.total.setText(Money.peso(o.total));

        boolean cancellable = "Processing".equals(o.status);
        h.cancel.setVisibility(cancellable ? View.VISIBLE : View.GONE);
        h.cancel.setOnClickListener(v -> listener.onCancelOrder(o));

        boolean delivered = Order.DELIVERED.equals(o.status);
        h.rate.setVisibility(delivered ? View.VISIBLE : View.GONE);
        h.rate.setOnClickListener(v -> listener.onRateOrder(o));
        boolean reportable = (delivered || Order.OUT.equals(o.status) || Order.PACKED.equals(o.status))
                && !o.hasOpenDispute();
        h.report.setVisibility(reportable ? View.VISIBLE : View.GONE);
        h.report.setOnClickListener(v -> listener.onReportProblem(o));

        h.lines.removeAllViews();
        for (Order.Line l : o.lines) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, UiUtil.dp(context, 2), 0, UiUtil.dp(context, 2));

            TextView left = new TextView(context);
            left.setText(l.quantity + " × " + l.name + " (" + l.color + ")");
            left.setTextSize(14);
            left.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            left.setTypeface(ResourcesCompat.getFont(context, R.font.inter_medium));
            row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView right = new TextView(context);
            right.setText(Money.peso(l.quantity * l.unitPrice));
            right.setTextSize(14);
            right.setGravity(Gravity.END);
            right.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            right.setTypeface(ResourcesCompat.getFont(context, R.font.inter_medium));
            row.addView(right, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            h.lines.addView(row);
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView code, status, date, shipTo, payment, total, extra;
        final LinearLayout lines;
        final View cancel, rate, report;

        VH(@NonNull View v) {
            super(v);
            code = v.findViewById(R.id.tvCode);
            status = v.findViewById(R.id.tvStatus);
            date = v.findViewById(R.id.tvDate);
            shipTo = v.findViewById(R.id.tvShipTo);
            payment = v.findViewById(R.id.tvPayment);
            total = v.findViewById(R.id.tvTotal);
            lines = v.findViewById(R.id.linesContainer);
            cancel = v.findViewById(R.id.btnCancelOrder);
            rate = v.findViewById(R.id.btnRate);
            report = v.findViewById(R.id.btnReport);
            extra = v.findViewById(R.id.tvExtra);
        }
    }
}
