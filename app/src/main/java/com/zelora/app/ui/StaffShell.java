package com.zelora.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zelora.app.R;
import com.zelora.app.db.UserDao;
import com.zelora.app.model.User;
import com.zelora.app.util.SessionManager;

// shell
public abstract class StaffShell extends AppCompatActivity {
    protected abstract String requiredRole();
    protected abstract int navMenu();
    protected abstract int defaultTab();
    protected abstract View buildPage(int menuId, LayoutInflater inflater, ViewGroup parent);
    protected int attentionCount() { return 0; }
    protected String attentionText(int n) { return n + " item(s) need your attention."; }

    protected User user;
    protected int userId;
    protected SessionManager session;
    private int currentTab;
    private BottomNavigationView nav;
    private ViewGroup content;

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
        setContentView(R.layout.activity_shell);
        content = findViewById(R.id.shellContent);

        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(this, v);
            pm.getMenu().add(User.roleLabel(user.role) + ": " + user.fullName).setEnabled(false);
            pm.getMenu().add("Log out");
            pm.setOnMenuItemClickListener(item -> {
                if ("Log out".contentEquals(item.getTitle())) confirmLogout();
                return true;
            });
            pm.show();
        });
        findViewById(R.id.btnBell).setOnClickListener(v -> {
            int n = attentionCount();
            UiUtil.toast(this, n > 0 ? attentionText(n) : "You are all caught up.");
        });

        currentTab = savedInstanceState != null ? savedInstanceState.getInt("tab", defaultTab()) : defaultTab();
        nav = findViewById(R.id.shellNav);
        nav.inflateMenu(navMenu());
        nav.setOnItemSelectedListener(item -> {
            currentTab = item.getItemId();
            refresh();
            return true;
        });
        nav.setSelectedItemId(currentTab);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", currentTab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (user == null) return;
        User now = new UserDao(this).getById(userId);
        if (now == null || !now.active || !now.role.equals(requiredRole())) {
            session.logout();
            Nav.toLogin(this);
            return;
        }
        refresh();
    }

    protected void refresh() {
        if (content == null) return;
        content.removeAllViews();
        content.addView(buildPage(currentTab, LayoutInflater.from(this), content));
        findViewById(R.id.bellDot).setVisibility(attentionCount() > 0 ? View.VISIBLE : View.GONE);
    }

    protected void goToTab(int menuId) {
        nav.setSelectedItemId(menuId);
    }

    protected void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out", (d, w) -> {
                    session.logout();
                    Nav.toLogin(this);
                })
                .show();
    }

    protected void confirm(String title, String message, String yes, Runnable onYes) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(yes, (d, w) -> onYes.run())
                .show();
    }
}
