package com.zelora.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zelora.app.R;
import com.zelora.app.db.UserDao;
import com.zelora.app.model.Order;
import com.zelora.app.model.User;
import com.zelora.app.util.Money;
import com.zelora.app.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// staff
public abstract class StaffActivity extends AppCompatActivity {

    public abstract static class Tab {
        public abstract List<CardRow> load();
        public String actionLabel() { return null; }
        public void onAction() { }
        public String emptyText() { return "Nothing here yet."; }
    }

    protected abstract String requiredRole();
    protected abstract String[] tabTitles();
    protected abstract Tab createTab(int index);

    protected User user;
    protected int userId;
    protected SessionManager session;

    private int selected = 0;
    private Tab tab;
    private final List<TextView> chips = new ArrayList<>();
    private ViewGroupHolder holder;

    private static class ViewGroupHolder {
        RecyclerView list;
        TextView empty, action;
        CardAdapter adapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        user = session.isLoggedIn() ? new UserDao(this).getById(session.getUserId()) : null;
        if (user == null || !user.active || !user.role.equals(requiredRole())) {
            if (user != null && !user.active) UiUtil.toast(this, "This account has been disabled.");
            session.logout();
            Nav.toLogin(this);
            return;
        }
        userId = user.id;
        setContentView(R.layout.activity_staff);
        ((TextView) findViewById(R.id.tvRole)).setText(User.roleLabel(user.role) + " • " + user.fullName);
        findViewById(R.id.btnStaffLogout).setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out", (d, w) -> {
                    session.logout();
                    Nav.toLogin(this);
                })
                .show());

        if (savedInstanceState != null) selected = savedInstanceState.getInt("tab", 0);
        buildChips();
        showTab(selected);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", selected);
    }

    @Override
    protected void onResume() {
        super.onResume();
        User now = new UserDao(this).getById(userId);
        if (now == null || !now.active || !now.role.equals(requiredRole())) {
            session.logout();
            Nav.toLogin(this);
            return;
        }
        refresh();
    }

    private void buildChips() {
        LinearLayout bar = findViewById(R.id.tabBar);
        String[] titles = tabTitles();
        for (int i = 0; i < titles.length; i++) {
            final int index = i;
            TextView chip = new TextView(this);
            chip.setText(titles[i]);
            chip.setTextSize(13);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setPadding(UiUtil.dp(this, 16), 0, UiUtil.dp(this, 16), 0);
            chip.setTypeface(ResourcesCompat.getFont(this, R.font.inter_medium));
            chip.setOnClickListener(v -> showTab(index));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, UiUtil.dp(this, 34));
            lp.setMarginEnd(UiUtil.dp(this, 8));
            bar.addView(chip, lp);
            chips.add(chip);
        }
    }

    private void showTab(int index) {
        selected = index;
        for (int i = 0; i < chips.size(); i++) {
            boolean on = i == index;
            TextView c = chips.get(i);
            c.setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
            c.setTextColor(ContextCompat.getColor(this, on ? R.color.white : R.color.text_primary));
        }
        tab = createTab(index);

        android.view.ViewGroup box = findViewById(R.id.staffContainer);
        box.removeAllViews();
        View content = LayoutInflater.from(this).inflate(R.layout.fragment_card_list, box, false);
        box.addView(content);
        holder = new ViewGroupHolder();
        holder.list = content.findViewById(R.id.rvCards);
        holder.empty = content.findViewById(R.id.tvEmpty);
        holder.action = content.findViewById(R.id.btnAction);
        holder.adapter = new CardAdapter(this);
        holder.list.setLayoutManager(new LinearLayoutManager(this));
        holder.list.setAdapter(holder.adapter);

        String label = tab.actionLabel();
        if (label != null) {
            holder.action.setText(label);
            holder.action.setVisibility(View.VISIBLE);
            holder.action.setOnClickListener(v -> tab.onAction());
        }
        refresh();
    }

    protected void refresh() {
        if (tab == null || holder == null) return;
        List<CardRow> rows = tab.load();
        holder.adapter.submit(rows);
        holder.empty.setText(tab.emptyText());
        holder.empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    protected void confirm(String title, String message, String yes, Runnable onYes) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(yes, (d, w) -> onYes.run())
                .show();
    }

    protected void askText(String title, String hint, java.util.function.Consumer<String> onOk) {
        final EditText et = new EditText(this);
        et.setHint(hint);
        et.setSingleLine(false);
        et.setMinLines(2);
        LinearLayout box = new LinearLayout(this);
        int pad = UiUtil.dp(this, 24);
        box.setPadding(pad, UiUtil.dp(this, 8), pad, 0);
        box.addView(et, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", (d, w) -> onOk.accept(et.getText().toString()))
                .show();
    }

    protected static CardRow orderCard(Order o, boolean showCustomer, int valueOverride) {
        String sub = (showCustomer && o.customerName != null ? o.customerName + " • " : "") + UiUtil.formatDate(o.createdAt);
        return new CardRow(o.code)
                .badge(o.status.toUpperCase(Locale.US))
                .value(Money.peso(valueOverride >= 0 ? valueOverride : o.total))
                .subtitle(sub)
                .body(OrderText.details(o));
    }
}
