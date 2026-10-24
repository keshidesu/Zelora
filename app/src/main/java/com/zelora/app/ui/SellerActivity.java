package com.zelora.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zelora.app.R;
import com.zelora.app.db.OrderDao;
import com.zelora.app.db.ProductDao;
import com.zelora.app.model.Order;
import com.zelora.app.model.Product;
import com.zelora.app.model.User;
import com.zelora.app.util.Money;
import com.zelora.app.util.ShopProfile;
import com.zelora.app.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// staff
public class SellerActivity extends StaffShell {
    private static final String[] CATEGORIES = {"Bags", "Apparel", "Accessories"};
    private static final String[] COLORS = {"Blue", "Cream", "Brown", "Yellow", "Red"};
    private static final String[] PHOTOS = {"img_mug", "img_sneakers", "img_sweater", "img_tote"};
    private static final String[] PHOTO_NAMES = {"Ceramic mug", "Sneakers", "Sweater", "Tote bag"};
    private static final String[] FILTER_STATUS = {Order.PROCESSING, Order.PREPARING, Order.PACKED};

    private int orderFilter = 0;

    @Override protected String requiredRole() { return User.SELLER; }
    @Override protected int navMenu() { return R.menu.seller_nav; }
    @Override protected int defaultTab() { return R.id.nav_overview; }

    @Override
    protected int attentionCount() {
        return new OrderDao(this).countForSeller(userId, Order.PROCESSING);
    }

    @Override
    protected String attentionText(int n) {
        return n + (n == 1 ? " new order is" : " new orders are") + " waiting for you.";
    }

    @Override
    protected View buildPage(int menuId, LayoutInflater inflater, ViewGroup parent) {
        if (menuId == R.id.nav_orders) return ordersPage(inflater, parent);
        if (menuId == R.id.nav_products) return productsPage(inflater, parent);
        if (menuId == R.id.nav_shop) return shopPage(inflater, parent);
        return overviewPage(inflater, parent);
    }

    private View overviewPage(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.page_seller_overview, parent, false);
        OrderDao dao = new OrderDao(this);

        ((TextView) v.findViewById(R.id.tvGreeting)).setText(UiUtil.greeting() + ", " + user.fullName);
        ((TextView) v.findViewById(R.id.tvShopName)).setText(ShopProfile.shopName(this, userId));

        int fresh = dao.countForSeller(userId, Order.PROCESSING);
        int[] week = dao.dailySales(userId, 7, 0);
        int[] before = dao.dailySales(userId, 7, 7);
        int weekTotal = 0, beforeTotal = 0;
        for (int x : week) weekTotal += x;
        for (int x : before) beforeTotal += x;

        ((TextView) v.findViewById(R.id.tvStatNew)).setText(String.valueOf(fresh));
        ((TextView) v.findViewById(R.id.tvStatSales)).setText(Money.peso(week[6]));
        ((TextView) v.findViewById(R.id.tvStatRating)).setText(shopRating());

        ((TextView) v.findViewById(R.id.tvWeekTotal)).setText(Money.peso(weekTotal));
        TextView change = v.findViewById(R.id.tvWeekChange);
        if (beforeTotal > 0) {
            int pct = Math.round((weekTotal - beforeTotal) * 100f / beforeTotal);
            change.setText((pct >= 0 ? "+" : "") + pct + "%");
            change.setTextColor(ContextCompat.getColor(this, pct >= 0 ? R.color.success : R.color.error));
        } else {
            change.setText("");
        }
        ((SalesChartView) v.findViewById(R.id.salesChart)).setValues(week);

        TextView title = v.findViewById(R.id.tvAttentionTitle);
        TextView sub = v.findViewById(R.id.tvAttentionSub);
        if (fresh > 0) {
            title.setText(fresh + (fresh == 1 ? " order to prepare" : " orders to prepare"));
            sub.setText("Pack and get them ready for delivery.");
        } else {
            title.setText("All caught up");
            sub.setText("No new orders are waiting right now.");
        }
        v.findViewById(R.id.btnViewOrders).setOnClickListener(x -> {
            orderFilter = 0;
            goToTab(R.id.nav_orders);
        });
        return v;
    }

    private String shopRating() {
        double sum = 0;
        int reviews = 0;
        for (Product p : new ProductDao(this).getBySeller(userId)) {
            sum += p.rating * p.reviewCount;
            reviews += p.reviewCount;
        }
        return reviews == 0 ? "-" : String.format(Locale.US, "%.1f", sum / reviews);
    }

    private View ordersPage(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.page_seller_orders, parent, false);
        OrderDao dao = new OrderDao(this);
        List<Order> all = dao.getForSeller(userId);

        int[] counts = new int[3];
        for (Order o : all) for (int i = 0; i < 3; i++) if (FILTER_STATUS[i].equals(o.status)) counts[i]++;

        String[] names = {"New", "Preparing", "Ready"};
        LinearLayout chips = v.findViewById(R.id.orderChips);
        for (int i = 0; i < 3; i++) {
            final int index = i;
            TextView chip = new TextView(this);
            chip.setText(names[i] + " (" + counts[i] + ")");
            chip.setTextSize(14);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setTypeface(ResourcesCompat.getFont(this, R.font.inter_medium));
            boolean on = i == orderFilter;
            chip.setBackgroundResource(on ? R.drawable.bg_chip_pill_on : R.drawable.bg_chip_pill_off);
            chip.setTextColor(ContextCompat.getColor(this, on ? R.color.white : R.color.text_dark));
            chip.setOnClickListener(x -> {
                orderFilter = index;
                refresh();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) lp.setMarginStart(UiUtil.dp(this, 8));
            chips.addView(chip, lp);
        }

        LinearLayout list = v.findViewById(R.id.orderList);
        int shown = 0;
        for (Order o : all) {
            if (!FILTER_STATUS[orderFilter].equals(o.status)) continue;
            list.addView(orderCard(inflater, list, o));
            shown++;
        }
        TextView empty = v.findViewById(R.id.tvOrdersEmpty);
        empty.setText(orderFilter == 0 ? "No new orders right now."
                : orderFilter == 1 ? "Nothing is being prepared." : "No orders are waiting for a rider.");
        empty.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
        return v;
    }

    private View orderCard(LayoutInflater inflater, ViewGroup parent, final Order o) {
        View card = inflater.inflate(R.layout.item_seller_order, parent, false);
        ((TextView) card.findViewById(R.id.tvCode)).setText("Order " + o.code);
        ((TextView) card.findViewById(R.id.tvAgo)).setText(UiUtil.timeAgo(o.createdAt));
        ((TextView) card.findViewById(R.id.tvCustomer)).setText(o.customerName == null ? "Customer" : o.customerName);

        int mine = 0, qty = 0;
        for (Order.Line l : o.lines) {
            mine += l.quantity * l.unitPrice;
            qty += l.quantity;
        }
        Order.Line first = o.lines.get(0);
        ((TextView) card.findViewById(R.id.tvProduct)).setText(
                o.lines.size() > 1 ? first.name + " +" + (o.lines.size() - 1) + " more" : first.name);
        ((TextView) card.findViewById(R.id.tvQty)).setText("Qty: " + qty);
        ((TextView) card.findViewById(R.id.tvPrice)).setText(Money.peso(mine));
        Product p = new ProductDao(this).getById(first.productId);
        if (p != null) ((ImageView) card.findViewById(R.id.ivThumb)).setImageResource(p.thumbRes(this));

        TextView primary = card.findViewById(R.id.btnPrimary);
        if (Order.PROCESSING.equals(o.status)) {
            primary.setText("✓  Accept order");
            primary.setOnClickListener(x -> confirm("Accept order?",
                    "Accept " + o.code + " and start preparing it? The customer can no longer cancel it.", "Accept", () -> {
                        boolean ok = new OrderDao(this).acceptOrder(userId, o.id);
                        UiUtil.toast(this, ok ? "Order accepted. Start preparing it." : "This order can no longer be accepted.");
                        refresh();
                    }));
        } else if (Order.PREPARING.equals(o.status)) {
            primary.setText("✓  Mark ready");
            primary.setOnClickListener(x -> confirm("Mark as ready?",
                    "Is " + o.code + " packed? A rider can then pick it up.", "Ready", () -> {
                        boolean ok = new OrderDao(this).markReady(userId, o.id);
                        UiUtil.toast(this, ok ? "Order is ready for a rider." : "This order can no longer be changed.");
                        refresh();
                    }));
        } else {
            primary.setText("Waiting for rider");
            primary.setBackgroundResource(R.drawable.bg_chip_pill_off);
            primary.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            primary.setClickable(false);
        }
        card.findViewById(R.id.btnDetails).setOnClickListener(x -> new MaterialAlertDialogBuilder(this)
                .setTitle("Order " + o.code)
                .setMessage(o.status + "\n\n" + OrderText.details(o))
                .setPositiveButton("Close", null)
                .show());
        return card;
    }

    private View productsPage(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.fragment_card_list, parent, false);
        RecyclerView list = v.findViewById(R.id.rvCards);
        TextView empty = v.findViewById(R.id.tvEmpty);
        TextView add = v.findViewById(R.id.btnAction);
        CardAdapter adapter = new CardAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        add.setText("+  Add a product");
        add.setVisibility(View.VISIBLE);
        add.setOnClickListener(x -> showProductDialog(null));

        List<CardRow> rows = new ArrayList<>();
        for (final Product p : new ProductDao(this).getBySeller(userId)) {
            CardRow r = new CardRow(p.name)
                    .badge(p.active ? "LISTED" : "UNLISTED")
                    .value(Money.peso(p.price))
                    .subtitle(p.category + " • Stock: " + p.stock + (p.stock <= 5 ? " (low)" : ""))
                    .body("Rating " + p.rating + " (" + p.reviewCount + " reviews)");
            r.action("Edit", false, () -> showProductDialog(p));
            r.action(p.active ? "Unlist" : "List again", !p.active, () -> {
                new ProductDao(this).setActive(userId, p.id, !p.active);
                refresh();
            });
            rows.add(r);
        }
        adapter.submit(rows);
        empty.setText("You have no products yet. Tap \"Add a product\".");
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        return v;
    }

    private void showProductDialog(final Product existing) {
        View form = getLayoutInflater().inflate(R.layout.dialog_product, null);
        final EditText etName = form.findViewById(R.id.etName);
        final EditText etPrice = form.findViewById(R.id.etPrice);
        final EditText etOriginal = form.findViewById(R.id.etOriginal);
        final EditText etStock = form.findViewById(R.id.etStock);
        final EditText etDesc = form.findViewById(R.id.etDescription);
        final TextView tvCategory = form.findViewById(R.id.tvCategory);
        final TextView tvColors = form.findViewById(R.id.tvColors);
        final TextView tvPhoto = form.findViewById(R.id.tvPhoto);

        final int[] category = {0};
        final int[] photo = {0};
        final boolean[] colors = new boolean[COLORS.length];
        colors[0] = true;

        if (existing != null) {
            etName.setText(existing.name);
            etPrice.setText(String.valueOf(existing.price));
            if (existing.originalPrice > 0) etOriginal.setText(String.valueOf(existing.originalPrice));
            etStock.setText(String.valueOf(existing.stock));
            etDesc.setText(existing.description);
            for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i].equals(existing.category)) category[0] = i;
            for (int i = 0; i < PHOTOS.length; i++) if (PHOTOS[i].equals(existing.gallery[0])) photo[0] = i;
            java.util.Arrays.fill(colors, false);
            for (String c : existing.colors) for (int i = 0; i < COLORS.length; i++) if (COLORS[i].equals(c)) colors[i] = true;
        }
        final Runnable redraw = () -> {
            tvCategory.setText("Category:  " + CATEGORIES[category[0]]);
            tvPhoto.setText("Photo:  " + PHOTO_NAMES[photo[0]]);
            tvColors.setText("Colors:  " + chosenColors(colors));
        };
        redraw.run();

        tvCategory.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Category")
                .setSingleChoiceItems(CATEGORIES, category[0], (d, which) -> {
                    category[0] = which;
                    redraw.run();
                    d.dismiss();
                }).show());
        tvPhoto.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Photo")
                .setSingleChoiceItems(PHOTO_NAMES, photo[0], (d, which) -> {
                    photo[0] = which;
                    redraw.run();
                    d.dismiss();
                }).show());
        tvColors.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Colors (pick at least one)")
                .setMultiChoiceItems(COLORS, colors, (d, which, checked) -> colors[which] = checked)
                .setPositiveButton("Done", (d, w) -> redraw.run())
                .show());

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? "Add a product" : "Edit product")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String e = Validator.productName(etName.getText().toString());
            if (e != null) { etName.setError(e); return; }
            e = Validator.price(etPrice.getText().toString());
            if (e != null) { etPrice.setError(e); return; }
            int price = Integer.parseInt(etPrice.getText().toString().trim());
            e = Validator.originalPrice(etOriginal.getText().toString(), price);
            if (e != null) { etOriginal.setError(e); return; }
            e = Validator.stock(etStock.getText().toString());
            if (e != null) { etStock.setError(e); return; }
            e = Validator.productDescription(etDesc.getText().toString());
            if (e != null) { etDesc.setError(e); return; }
            String picked = chosenColors(colors);
            if (picked.isEmpty()) { UiUtil.toast(this, "Pick at least one color."); return; }

            int original = etOriginal.getText().toString().trim().isEmpty() ? 0
                    : Integer.parseInt(etOriginal.getText().toString().trim());
            int stock = Integer.parseInt(etStock.getText().toString().trim());
            try {
                ProductDao dao = new ProductDao(this);
                if (existing == null) {
                    dao.insert(userId, etName.getText().toString(), CATEGORIES[category[0]], etDesc.getText().toString(),
                            price, original, PHOTOS[photo[0]], picked, stock);
                } else {
                    dao.update(userId, existing.id, etName.getText().toString(), CATEGORIES[category[0]],
                            etDesc.getText().toString(), price, original, PHOTOS[photo[0]], picked, stock);
                }
                dialog.dismiss();
                UiUtil.toast(this, existing == null ? "Product added." : "Product saved.");
                refresh();
            } catch (Exception ex) {
                UiUtil.toast(this, "Could not save the product. Please try again.");
            }
        });
    }

    private static String chosenColors(boolean[] on) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < COLORS.length; i++) {
            if (!on[i]) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(COLORS[i]);
        }
        return sb.toString();
    }

    private View shopPage(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.fragment_card_list, parent, false);
        RecyclerView list = v.findViewById(R.id.rvCards);
        CardAdapter adapter = new CardAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        List<CardRow> rows = new ArrayList<>();
        CardRow shop = new CardRow(ShopProfile.shopName(this, userId))
                .subtitle("Your shop name • " + user.email)
                .body("Owner: " + user.fullName);
        shop.action("Rename shop", false, () -> askShopName());
        shop.action("Log out", false, () -> confirmLogout());
        rows.add(shop);

        for (Map.Entry<String, String> e : new OrderDao(this).sellerStats(userId).entrySet()) {
            CardRow r = new CardRow(e.getKey());
            if (e.getValue().length() > 12) r.body(e.getValue()); else r.value(e.getValue());
            rows.add(r);
        }
        adapter.submit(rows);
        return v;
    }

    private void askShopName() {
        final EditText et = new EditText(this);
        et.setHint("Shop name");
        et.setSingleLine(true);
        et.setText(ShopProfile.shopName(this, userId));
        LinearLayout box = new LinearLayout(this);
        int pad = UiUtil.dp(this, 24);
        box.setPadding(pad, UiUtil.dp(this, 8), pad, 0);
        box.addView(et, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        new MaterialAlertDialogBuilder(this)
                .setTitle("Rename shop")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (name.length() < 2 || name.length() > 40) {
                        UiUtil.toast(this, "The shop name must be 2 to 40 characters.");
                        return;
                    }
                    ShopProfile.setShopName(this, userId, name);
                    refresh();
                })
                .show();
    }
}
