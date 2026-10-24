package com.zelora.app.ui;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zelora.app.R;
import com.zelora.app.db.OrderDao;
import com.zelora.app.model.Order;
import com.zelora.app.model.User;
import com.zelora.app.util.Money;
import com.zelora.app.util.ShopProfile;

import java.util.ArrayList;
import java.util.List;

// staff
public class RiderActivity extends StaffShell {

    @Override protected String requiredRole() { return User.RIDER; }
    @Override protected int navMenu() { return R.menu.rider_nav; }
    @Override protected int defaultTab() { return R.id.nav_rhome; }

    @Override
    protected int attentionCount() {
        return ShopProfile.isOnline(this, userId) ? new OrderDao(this).getAvailableJobs(userId).size() : 0;
    }

    @Override
    protected String attentionText(int n) {
        return n + (n == 1 ? " delivery request is" : " delivery requests are") + " waiting for you.";
    }

    @Override
    protected View buildPage(int menuId, LayoutInflater inflater, ViewGroup parent) {
        if (menuId == R.id.nav_tasks) return tasksPage(inflater, parent);
        if (menuId == R.id.nav_wallet) return walletPage(inflater, parent);
        if (menuId == R.id.nav_rprofile) return profilePage(inflater, parent);
        return homePage(inflater, parent);
    }

    private View homePage(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.page_rider_home, parent, false);
        final OrderDao dao = new OrderDao(this);

        ((TextView) v.findViewById(R.id.tvInitials)).setText(user.initials());
        ((TextView) v.findViewById(R.id.tvHello)).setText(UiUtil.greeting() + ",");
        String[] parts = user.fullName.trim().split("\\s+");
        ((TextView) v.findViewById(R.id.tvName)).setText(parts[0]);

        final boolean online = ShopProfile.isOnline(this, userId);
        Switch sw = v.findViewById(R.id.swOnline);
        sw.setChecked(online);
        sw.setOnCheckedChangeListener((b, checked) -> {
            ShopProfile.setOnline(this, userId, checked);
            refresh();
        });

        int today = dao.deliveriesToday(userId);
        ((TextView) v.findViewById(R.id.tvEarnings)).setText(Money.peso(today * Money.RIDER_FEE));
        ((TextView) v.findViewById(R.id.tvStatDeliveries)).setText(String.valueOf(today));
        ((TextView) v.findViewById(R.id.tvStatActive)).setText(String.valueOf(dao.activeDeliveries(userId)));
        v.findViewById(R.id.cardEarnings).setOnClickListener(x -> goToTab(R.id.nav_wallet));

        android.widget.LinearLayout box = v.findViewById(R.id.taskList);
        int shown = 0;
        for (Order o : dao.getRiderDeliveries(userId)) {
            if (!Order.OUT.equals(o.status)) continue;
            box.addView(taskCard(inflater, box, o, true));
            shown++;
        }
        if (online) {
            int jobs = 0;
            for (Order o : dao.getAvailableJobs(userId)) {
                if (jobs++ == 2) break;
                box.addView(taskCard(inflater, box, o, false));
                shown++;
            }
        }
        TextView none = v.findViewById(R.id.tvNoTasks);
        none.setText(online ? "No delivery requests right now.\nNew ones appear here." : "You are offline.\nSwitch Online to receive requests.");
        none.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
        return v;
    }

    private View taskCard(LayoutInflater inflater, ViewGroup parent, final Order o, boolean inProgress) {
        View card = inflater.inflate(R.layout.item_task, parent, false);
        ((TextView) card.findViewById(R.id.tvTag)).setText(inProgress ? "In progress" : "Incoming Request");
        ((TextView) card.findViewById(R.id.tvCode)).setText(o.code);
        int seller = new OrderDao(this).firstSellerId(o.id);
        ((TextView) card.findViewById(R.id.tvPickup)).setText("Pickup: " + ShopProfile.shopName(this, seller));
        ((TextView) card.findViewById(R.id.tvDropoff)).setText("Drop-off: " + o.address);
        String pay = "PAID".equals(o.paymentStatus) ? "Paid online" : "Collect cash";
        ((TextView) card.findViewById(R.id.tvAmount)).setText(Money.peso(o.total) + " · " + pay);

        TextView primary = card.findViewById(R.id.btnPrimary);
        if (inProgress) {
            primary.setText("Continue");
            primary.setOnClickListener(x -> openDelivery(o.id));
        } else {
            primary.setText("Accept");
            primary.setOnClickListener(x -> acceptJob(o));
        }
        card.findViewById(R.id.btnDetails).setOnClickListener(x -> new MaterialAlertDialogBuilder(this)
                .setTitle("Order " + o.code)
                .setMessage(OrderText.details(o))
                .setPositiveButton("Close", null)
                .show());
        return card;
    }

    private void acceptJob(Order o) {
        if (new OrderDao(this).acceptJob(userId, o.id)) {
            openDelivery(o.id);
        } else {
            UiUtil.toast(this, "Another rider already took this job.");
        }
        refresh();
    }

    private void openDelivery(int orderId) {
        Intent i = new Intent(this, DeliveryActivity.class);
        i.putExtra(DeliveryActivity.EXTRA_ORDER_ID, orderId);
        startActivity(i);
    }

    private View tasksPage(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.fragment_card_list, parent, false);
        RecyclerView list = v.findViewById(R.id.rvCards);
        TextView empty = v.findViewById(R.id.tvEmpty);
        CardAdapter adapter = new CardAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        OrderDao dao = new OrderDao(this);
        List<CardRow> rows = new ArrayList<>();
        List<Order> mine = dao.getRiderDeliveries(userId);
        for (final Order o : mine) {
            if (!Order.OUT.equals(o.status)) continue;
            CardRow r = StaffActivity.orderCard(o, true, -1);
            r.action("Open delivery", true, () -> openDelivery(o.id));
            rows.add(r);
        }
        if (ShopProfile.isOnline(this, userId)) {
            for (final Order o : dao.getAvailableJobs(userId)) {
                CardRow r = StaffActivity.orderCard(o, true, -1);
                r.action("Accept job", true, () -> acceptJob(o));
                r.action("Reject", false, () -> {
                    new OrderDao(this).rejectJob(userId, o.id);
                    refresh();
                });
                rows.add(r);
            }
        }
        for (Order o : mine) {
            if (Order.OUT.equals(o.status)) continue;
            rows.add(StaffActivity.orderCard(o, true, -1));
        }
        adapter.submit(rows);
        empty.setText(ShopProfile.isOnline(this, userId) ? "No tasks yet." : "You are offline. Switch Online on the Home tab.");
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        return v;
    }

    private View walletPage(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.fragment_card_list, parent, false);
        RecyclerView list = v.findViewById(R.id.rvCards);
        CardAdapter adapter = new CardAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        OrderDao dao = new OrderDao(this);
        int total = dao.deliveriesTotal(userId);
        List<CardRow> rows = new ArrayList<>();
        rows.add(new CardRow("Total earnings")
                .value(Money.peso(total * Money.RIDER_FEE))
                .body(total + " completed deliveries × " + Money.peso(Money.RIDER_FEE) + " each.\nToday: "
                        + Money.peso(dao.deliveriesToday(userId) * Money.RIDER_FEE)));
        for (Order o : dao.getRiderDeliveries(userId)) {
            if (!Order.DELIVERED.equals(o.status)) continue;
            String cash = "PAID".equals(o.paymentStatus) && "Cash on Delivery".equalsIgnoreCase(String.valueOf(o.paymentMethod))
                    ? " (cash collected: " + Money.peso(o.total) + ")" : "";
            rows.add(new CardRow(o.code)
                    .value("+" + Money.peso(Money.RIDER_FEE))
                    .subtitle(UiUtil.formatDate(o.deliveredAt == null ? o.createdAt : o.deliveredAt))
                    .body("Delivered to " + o.recipient + cash));
        }
        adapter.submit(rows);
        return v;
    }

    private View profilePage(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.fragment_card_list, parent, false);
        RecyclerView list = v.findViewById(R.id.rvCards);
        CardAdapter adapter = new CardAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        List<CardRow> rows = new ArrayList<>();
        CardRow me = new CardRow(user.fullName)
                .badge("RIDER")
                .subtitle(user.email)
                .body(user.phone.isEmpty() ? "No phone number saved." : "Phone: " + user.phone);
        me.action("Log out", false, () -> confirmLogout());
        rows.add(me);
        rows.add(new CardRow("Deliveries completed").value(String.valueOf(new OrderDao(this).deliveriesTotal(userId))));
        adapter.submit(rows);
        return v;
    }
}
