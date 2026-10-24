package com.zelora.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zelora.app.R;
import com.zelora.app.db.OrderDao;
import com.zelora.app.db.UserDao;
import com.zelora.app.model.Order;
import com.zelora.app.model.User;
import com.zelora.app.util.SessionManager;

// ui
public class DeliveryActivity extends AppCompatActivity {
    public static final String EXTRA_ORDER_ID = "order_id";

    private int userId, orderId;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        User me = session.isLoggedIn() ? new UserDao(this).getById(session.getUserId()) : null;
        if (me == null || !me.active || !User.RIDER.equals(me.role)) {
            session.logout();
            Nav.toLogin(this);
            return;
        }
        userId = me.id;
        orderId = getIntent().getIntExtra(EXTRA_ORDER_ID, 0);
        setContentView(R.layout.activity_delivery);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId == 0) return;
        bind();
    }

    private void bind() {
        final Order o = new OrderDao(this).getRiderOrder(userId, orderId);
        if (o == null) {
            UiUtil.toast(this, "This delivery is not available.");
            finish();
            return;
        }
        boolean delivered = Order.DELIVERED.equals(o.status);
        boolean out = Order.OUT.equals(o.status);
        ((TextView) findViewById(R.id.tvTitle)).setText("Order " + o.code);
        ((TextView) findViewById(R.id.tvDeliverTo)).setText("Deliver to " + firstName(o.recipient));
        ((TextView) findViewById(R.id.tvAddress)).setText(o.address);

        ProgressStepView steps = findViewById(R.id.stepper);
        steps.setSteps(new String[]{"Accepted", "Picked up", "Delivered"},
                new boolean[]{true, o.pickedUp || delivered, delivered});

        TextView main = findViewById(R.id.btnMain);
        if (!out) {
            main.setText(delivered ? "Delivered ✓" : "Not available");
            main.setClickable(false);
        } else if (!o.pickedUp) {
            main.setText("Confirm pickup");
            main.setClickable(true);
            main.setOnClickListener(v -> {
                boolean ok = new OrderDao(this).confirmPickup(userId, o.id);
                UiUtil.toast(this, ok ? "Pickup confirmed. Safe ride!" : "Could not confirm pickup.");
                bind();
            });
        } else {
            main.setText("Mark as delivered");
            main.setClickable(true);
            main.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                    .setTitle("Delivered?")
                    .setMessage("Confirm that " + o.code + " was handed to the customer"
                            + ("PENDING".equals(o.paymentStatus) ? " and the cash was collected." : "."))
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delivered", (d, w) -> {
                        boolean ok = new OrderDao(this).markDelivered(userId, o.id);
                        UiUtil.toast(this, ok ? "Delivery completed." : "This order is no longer in delivery.");
                        bind();
                    })
                    .show());
        }

        View.OnClickListener contact = v -> callCustomer(o);
        findViewById(R.id.btnContact).setOnClickListener(contact);
        findViewById(R.id.btnChat).setOnClickListener(contact);
        findViewById(R.id.cardDeliverTo).setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Order " + o.code)
                .setMessage(OrderText.details(o))
                .setPositiveButton("Close", null)
                .show());
    }

    private void callCustomer(Order o) {
        User customer = new UserDao(this).getById(o.customerId);
        if (customer == null || customer.phone.isEmpty()) {
            UiUtil.toast(this, "The customer did not save a phone number.");
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(customer.phone))));
    }

    private static String firstName(String name) {
        String n = name == null ? "" : name.trim();
        int sp = n.indexOf(' ');
        return sp > 0 ? n.substring(0, sp) : n;
    }
}
