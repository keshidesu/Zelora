package com.zelora.app.ui;

import com.zelora.app.db.DisputeDao;
import com.zelora.app.db.OrderDao;
import com.zelora.app.db.UserDao;
import com.zelora.app.db.VoucherDao;
import com.zelora.app.model.Dispute;
import com.zelora.app.model.Order;
import com.zelora.app.model.User;
import com.zelora.app.model.Voucher;
import com.zelora.app.util.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// ui
public class AdminActivity extends StaffActivity {

    @Override protected String requiredRole() { return User.ADMIN; }
    @Override protected String[] tabTitles() { return new String[]{"Overview", "Users", "Orders", "Disputes", "Vouchers"}; }

    @Override
    protected Tab createTab(int index) {
        switch (index) {
            case 0:  return overviewTab();
            case 1:  return usersTab();
            case 2:  return ordersTab();
            case 3:  return disputesTab();
            default: return vouchersTab();
        }
    }

    private Tab overviewTab() {
        return new Tab() {
            @Override public List<CardRow> load() {
                List<CardRow> rows = new ArrayList<>();
                for (Map.Entry<String, String> e : new OrderDao(AdminActivity.this).adminStats().entrySet()) {
                    rows.add(new CardRow(e.getKey()).value(e.getValue()));
                }
                return rows;
            }
        };
    }

    private Tab usersTab() {
        return new Tab() {
            @Override public List<CardRow> load() {
                List<CardRow> rows = new ArrayList<>();
                for (final User u : new UserDao(AdminActivity.this).getAll()) {
                    CardRow r = new CardRow(u.fullName)
                            .badge(u.role)
                            .subtitle(u.email)
                            .body(u.active ? "Account is active" : "Account is DISABLED (cannot log in)");
                    if (!User.ADMIN.equals(u.role)) {
                        r.action(u.active ? "Disable account" : "Enable account", !u.active, () -> confirm(
                                u.active ? "Disable account?" : "Enable account?",
                                (u.active ? u.fullName + " will not be able to log in." : u.fullName + " can log in again."),
                                u.active ? "Disable" : "Enable",
                                () -> {
                                    new UserDao(AdminActivity.this).setActive(u.id, !u.active);
                                    refresh();
                                }));
                    }
                    rows.add(r);
                }
                return rows;
            }
        };
    }

    private Tab ordersTab() {
        return new Tab() {
            @Override public List<CardRow> load() {
                List<CardRow> rows = new ArrayList<>();
                for (Order o : new OrderDao(AdminActivity.this).getAllOrders()) rows.add(orderCard(o, true, -1));
                return rows;
            }
            @Override public String emptyText() { return "No orders have been placed yet."; }
        };
    }

    private Tab disputesTab() {
        return new Tab() {
            @Override public List<CardRow> load() {
                List<CardRow> rows = new ArrayList<>();
                for (final Dispute d : new DisputeDao(AdminActivity.this).getAll()) {
                    String body = "Reason: " + d.reason + "\n" + d.details
                            + (d.adminNote == null || d.adminNote.isEmpty() ? "" : "\n\nAdmin note: " + d.adminNote);
                    CardRow r = new CardRow("Order " + d.orderCode)
                            .badge(d.status.toUpperCase(java.util.Locale.US))
                            .subtitle(d.customerName + " • " + UiUtil.formatDate(d.createdAt))
                            .body(body);
                    if ("Open".equals(d.status)) {
                        r.action("Refund", true, () -> decide(d, true, true));
                        r.action("Resolve", false, () -> decide(d, true, false));
                        r.action("Reject", false, () -> decide(d, false, false));
                    }
                    rows.add(r);
                }
                return rows;
            }
            @Override public String emptyText() { return "No problem reports from customers."; }
        };
    }

    private void decide(final Dispute d, final boolean resolved, final boolean refund) {
        askText(resolved ? (refund ? "Refund and resolve" : "Resolve") : "Reject report",
                "Note for the customer (optional)", note -> {
                    boolean ok = new DisputeDao(this).decide(d.id, resolved, note, refund);
                    UiUtil.toast(this, ok ? "Saved." : "This report was already handled.");
                    refresh();
                });
    }

    private Tab vouchersTab() {
        return new Tab() {
            @Override public List<CardRow> load() {
                List<CardRow> rows = new ArrayList<>();
                for (Voucher v : new VoucherDao(AdminActivity.this).getActive()) {
                    String value = Voucher.PERCENT.equals(v.kind) ? v.value + "%"
                            : Voucher.FIXED.equals(v.kind) ? Money.peso(v.value) : "Free ship";
                    rows.add(new CardRow(v.code).value(value).subtitle(v.description)
                            .body("Minimum spend: " + Money.peso(v.minSpend) + " • one use per customer"));
                }
                return rows;
            }
        };
    }
}
