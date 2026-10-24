package com.zelora.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zelora.app.R;
import com.zelora.app.db.CartDao;
import com.zelora.app.db.UserDao;
import com.zelora.app.model.User;
import com.zelora.app.util.SessionManager;

// auth
public class MainActivity extends AppCompatActivity implements CheckoutBottomSheet.OnOrderPlacedListener {
    private DrawerLayout drawer;
    private BottomNavigationView bottomNav;
    private SessionManager session;
    private int currentTab = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }
        com.zelora.app.model.User me = new com.zelora.app.db.UserDao(this).getById(session.getUserId());
        if (me == null || !me.active) {
            session.logout();
            goToLogin();
            return;
        }
        if (!com.zelora.app.model.User.CUSTOMER.equals(me.role)) {
            Nav.openHome(this, me);
            return;
        }
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
        drawer = findViewById(R.id.drawerLayout);
        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        } else {
            currentTab = bottomNav.getSelectedItemId();
        }

        drawer.setScrimColor(ContextCompat.getColor(this, R.color.scrim));
        setupDrawer();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else if (currentTab != R.id.nav_home) {
                    bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session.isLoggedIn() && drawer != null) {
            User u = new UserDao(this).getById(session.getUserId());
            if (u == null || !u.active) {
                session.logout();
                goToLogin();
                return;
            }
            refreshBadges();
            com.zelora.app.util.NotificationHelper.checkOrders(this, u.id,
                    new com.zelora.app.db.OrderDao(this).getOrders(u.id));
        }
    }

    private void setupDrawer() {
        findViewById(R.id.btnCloseDrawer).setOnClickListener(v -> drawer.closeDrawer(GravityCompat.START));
        findViewById(R.id.menuOrders).setOnClickListener(v -> openFromDrawer(new Intent(this, OrdersActivity.class)));
        findViewById(R.id.menuSettings).setOnClickListener(v -> openFromDrawer(new Intent(this, AccountSettingsActivity.class)));
        findViewById(R.id.menuHelp).setOnClickListener(v -> openFromDrawer(InfoActivity.intent(this, InfoActivity.TYPE_HELP)));
        findViewById(R.id.menuTerms).setOnClickListener(v -> openFromDrawer(InfoActivity.intent(this, InfoActivity.TYPE_TERMS)));
        findViewById(R.id.menuAccount).setOnClickListener(v -> {
            drawer.closeDrawer(GravityCompat.START);
            bottomNav.setSelectedItemId(R.id.nav_account);
        });
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            drawer.closeDrawer(GravityCompat.START);
            confirmLogout();
        });
    }

    private void openFromDrawer(Intent intent) {
        drawer.closeDrawer(GravityCompat.START);
        startActivity(intent);
    }

    public void openDrawer() {
        drawer.openDrawer(GravityCompat.START);
    }

    public void selectTab(int menuId) {
        bottomNav.setSelectedItemId(menuId);
    }

    public void refreshBadges() {
        int count = new CartDao(this).count(session.getUserId());
        if (count > 0) {
            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_cart);
            badge.setBackgroundColor(ContextCompat.getColor(this, R.color.orange));
            badge.setVisible(true);
        } else {
            bottomNav.removeBadge(R.id.nav_cart);
        }
    }

    public void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out", (d, w) -> {
                    session.logout();
                    goToLogin();
                })
                .show();
    }

    @Override
    public void onOrderPlaced() {
        refreshBadges();
        showTab(currentTab);
    }

    private void showTab(int menuId) {
        currentTab = menuId;
        Fragment fragment;
        if (menuId == R.id.nav_wishlist) fragment = new WishlistFragment();
        else if (menuId == R.id.nav_cart) fragment = new CartFragment();
        else if (menuId == R.id.nav_account) fragment = new AccountFragment();
        else fragment = new HomeFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
