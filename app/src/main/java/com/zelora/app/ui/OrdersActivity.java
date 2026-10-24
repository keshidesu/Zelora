package com.zelora.app.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zelora.app.R;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.zelora.app.db.DisputeDao;
import com.zelora.app.db.OrderDao;
import com.zelora.app.db.ReviewDao;
import com.zelora.app.util.Validator;
import com.zelora.app.model.Order;
import com.zelora.app.util.SessionManager;

import java.util.List;

// order
public class OrdersActivity extends AppCompatActivity implements OrderAdapter.Listener {
    private OrderAdapter adapter;
    private View empty;
    private RecyclerView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);
        UiUtil.setupToolbar(this, "My Orders");

        empty = findViewById(R.id.emptyState);
        list = findViewById(R.id.rvOrders);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(this, this);
        list.setAdapter(adapter);
    }

    @Override
    public void onCancelOrder(final Order order) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel order?")
                .setMessage("Cancel order " + order.code + "? The items go back in stock"
                        + ("PAID".equals(order.paymentStatus) ? " and your payment will be refunded." : "."))
                .setNegativeButton("Keep Order", null)
                .setPositiveButton("Cancel Order", (d, w) -> {
                    boolean done = new OrderDao(this).cancelOrder(new SessionManager(this).getUserId(), order.id);
                    UiUtil.toast(this, done ? "Order cancelled." : "This order can no longer be cancelled.");
                    reload();
                })
                .show();
    }

    // // review

    @Override
    public void onRateOrder(final Order order) {
        if (order.lines.size() == 1) {
            showReviewDialog(order, order.lines.get(0));
            return;
        }
        String[] names = new String[order.lines.size()];
        for (int i = 0; i < names.length; i++) names[i] = order.lines.get(i).name;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Which product do you want to rate?")
                .setItems(names, (d, which) -> showReviewDialog(order, order.lines.get(which)))
                .show();
    }

    private void showReviewDialog(final Order order, final Order.Line line) {
        View form = getLayoutInflater().inflate(R.layout.dialog_review, null);
        ((TextView) form.findViewById(R.id.tvProduct)).setText(line.name);
        final RatingBar bar = form.findViewById(R.id.ratingBar);
        final EditText comment = form.findViewById(R.id.etComment);
        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Rate & Review")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int rating = Math.round(bar.getRating());
            if (rating < 1) { UiUtil.toast(this, "Tap the stars to rate."); return; }
            int result = new ReviewDao(this).add(new SessionManager(this).getUserId(), line.productId,
                    order.id, rating, comment.getText().toString());
            dialog.dismiss();
            if (result == ReviewDao.OK) UiUtil.toast(this, "Thank you for your review!");
            else if (result == ReviewDao.ALREADY_REVIEWED) UiUtil.toast(this, "You already reviewed this product.");
            else UiUtil.toast(this, "Only delivered items can be reviewed.");
        });
    }

    // // dispute

    @Override
    public void onReportProblem(final Order order) {
        final View form = getLayoutInflater().inflate(R.layout.dialog_dispute, null);
        final RadioGroup group = form.findViewById(R.id.rgReason);
        final EditText details = form.findViewById(R.id.etDetails);
        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Report a problem with " + order.code)
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send report", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String e = Validator.disputeDetails(details.getText().toString());
            if (e != null) { details.setError(e); return; }
            String reason = ((android.widget.RadioButton) form.findViewById(group.getCheckedRadioButtonId())).getText().toString();
            boolean ok = new DisputeDao(this).create(new SessionManager(this).getUserId(), order.id, reason,
                    details.getText().toString());
            dialog.dismiss();
            UiUtil.toast(this, ok ? "Report sent. The Zelora team will review it." : "A report for this order is already open.");
            reload();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        int uid = new SessionManager(this).getUserId();
        List<Order> orders = new OrderDao(this).getOrders(uid);
        com.zelora.app.util.NotificationHelper.checkOrders(this, uid, orders);
        adapter.submit(orders);
        empty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(orders.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
