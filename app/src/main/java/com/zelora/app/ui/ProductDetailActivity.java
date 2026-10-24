package com.zelora.app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zelora.app.R;
import com.zelora.app.db.CartDao;
import com.zelora.app.db.ProductDao;
import com.zelora.app.db.WishlistDao;
import com.zelora.app.model.CartItem;
import com.zelora.app.model.Product;
import com.zelora.app.util.ColorUtil;
import com.zelora.app.util.Money;
import com.zelora.app.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// product
public class ProductDetailActivity extends AppCompatActivity implements CheckoutBottomSheet.OnOrderPlacedListener {
    private static final String EXTRA_PRODUCT_ID = "product_id";

    public static Intent intent(Context c, int productId) {
        return new Intent(c, ProductDetailActivity.class).putExtra(EXTRA_PRODUCT_ID, productId);
    }

    private Product product;
    private int userId;
    private String selectedColor;
    private int quantity = 1;

    private CartDao cartDao;
    private WishlistDao wishlistDao;
    private ViewPager2 pager;
    private LinearLayout dots, swatches;
    private TextView tvColorName, tvQty;
    private ImageView btnWishlist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        userId = new SessionManager(this).getUserId();
        cartDao = new CartDao(this);
        wishlistDao = new WishlistDao(this);

        product = new ProductDao(this).getById(getIntent().getIntExtra(EXTRA_PRODUCT_ID, -1));
        if (product == null) {
            UiUtil.toast(this, "This product is no longer available.");
            finish();
            return;
        }
        selectedColor = product.defaultColor;

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnWishlist = findViewById(R.id.btnWishlist);
        btnWishlist.setOnClickListener(v -> toggleWishlist());
        findViewById(R.id.btnShare).setOnClickListener(v -> share());

        bindTexts();
        setupGallery();
        setupColors();
        setupQuantity();

        findViewById(R.id.btnAddToCart).setOnClickListener(v -> addToCart());
        findViewById(R.id.btnBuyNow).setOnClickListener(v -> buyNow());
        refreshHeart();
        showReviews();
    }

    // // review

    private void showReviews() {
        LinearLayout box = findViewById(R.id.reviewsContainer);
        box.removeAllViews();
        List<com.zelora.app.model.Review> list = new com.zelora.app.db.ReviewDao(this).forProduct(product.id, 5);
        if (list.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("No reviews yet. Buy it, then rate it from My Orders.");
            none.setTextSize(12.5f);
            none.setTextColor(ContextCompat.getColor(this, R.color.text_desc));
            box.addView(none);
            return;
        }
        for (com.zelora.app.model.Review r : list) {
            TextView t = new TextView(this);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) sb.append(i < r.rating ? "★" : "☆");
            sb.append("  ").append(r.reviewer);
            if (r.comment != null && !r.comment.isEmpty()) sb.append("\n").append(r.comment);
            t.setText(sb.toString());
            t.setTextSize(12.5f);
            t.setLineSpacing(UiUtil.dp(this, 3), 1f);
            t.setTextColor(ContextCompat.getColor(this, R.color.text_desc));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = UiUtil.dp(this, 10);
            box.addView(t, lp);
        }
    }

    // // product

    private void bindTexts() {
        ((TextView) findViewById(R.id.tvName)).setText(product.name);
        ((TextView) findViewById(R.id.tvRating)).setText(String.format(Locale.US, "%.1f", product.rating));
        ((TextView) findViewById(R.id.tvReviews)).setText("(" + product.reviewCount + " reviews)");
        ((TextView) findViewById(R.id.tvPrice)).setText(Money.peso(product.price));
        ((TextView) findViewById(R.id.tvDescription)).setText(product.description);

        TextView old = findViewById(R.id.tvOldPrice);
        TextView sale = findViewById(R.id.tvSale);
        if (product.hasDiscount()) {
            old.setText(Money.peso(product.originalPrice));
            old.setPaintFlags(old.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            sale.setText(product.discountPercent() + "% OFF");
        } else {
            old.setVisibility(View.GONE);
            sale.setVisibility(View.GONE);
        }
    }

    private void setupGallery() {
        List<Integer> images = new ArrayList<>();
        for (String name : product.gallery) images.add(Product.drawableRes(this, name));

        pager = findViewById(R.id.pagerImages);
        dots = findViewById(R.id.dotsContainer);
        pager.setAdapter(new GalleryAdapter(images));
        buildDots(images.size());
        updateDots(0);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });
        final int count = images.size();
        findViewById(R.id.btnPrev).setOnClickListener(v -> pager.setCurrentItem((pager.getCurrentItem() - 1 + count) % count, true));
        findViewById(R.id.btnNext).setOnClickListener(v -> pager.setCurrentItem((pager.getCurrentItem() + 1) % count, true));
    }

    private void buildDots(int count) {
        dots.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiUtil.dp(this, 4), UiUtil.dp(this, 4));
            lp.setMargins(UiUtil.dp(this, 3), 0, UiUtil.dp(this, 3), 0);
            dot.setLayoutParams(lp);
            dots.addView(dot);
        }
    }

    private void updateDots(int active) {
        for (int i = 0; i < dots.getChildCount(); i++) {
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
            d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            d.setColor(ContextCompat.getColor(this, i == active ? R.color.brown_dark : R.color.chevron));
            dots.getChildAt(i).setBackground(d);
        }
    }

    private void setupColors() {
        swatches = findViewById(R.id.swatchContainer);
        tvColorName = findViewById(R.id.tvColorName);
        drawSwatches();
        findViewById(R.id.rowColor).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Choose a color")
                        .setItems(product.colors, (d, which) -> {
                            selectedColor = product.colors[which];
                            drawSwatches();
                        })
                        .show());
    }

    private void drawSwatches() {
        tvColorName.setText(selectedColor);
        swatches.removeAllViews();
        for (final String name : product.colors) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiUtil.dp(this, 38), UiUtil.dp(this, 38));
            lp.setMarginEnd(UiUtil.dp(this, 6));
            lp.setMarginStart(swatches.getChildCount() == 0 ? UiUtil.dp(this, -2) : 0);
            dot.setLayoutParams(lp);
            dot.setBackground(ColorUtil.circle(this, name, name.equals(selectedColor)));
            dot.setContentDescription(name);
            dot.setOnClickListener(v -> {
                selectedColor = name;
                drawSwatches();
            });
            swatches.addView(dot);
        }
    }

    private int maxQuantity() {
        return Math.max(1, Math.min(CartDao.MAX_QTY, product.stock));
    }

    private void setupQuantity() {
        tvQty = findViewById(R.id.tvQty);
        tvQty.setText(String.valueOf(quantity));
        findViewById(R.id.btnMinus).setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQty.setText(String.valueOf(quantity));
            }
        });
        findViewById(R.id.btnPlus).setOnClickListener(v -> {
            if (quantity < maxQuantity()) {
                quantity++;
                tvQty.setText(String.valueOf(quantity));
            } else {
                UiUtil.toast(this, "Only " + maxQuantity() + " available.");
            }
        });
    }

    private void toggleWishlist() {
        boolean now = wishlistDao.toggle(userId, product.id);
        refreshHeart();
        UiUtil.toast(this, now ? "Added to wishlist" : "Removed from wishlist");
    }

    private void refreshHeart() {
        boolean wished = wishlistDao.contains(userId, product.id);
        btnWishlist.setImageResource(wished ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        ImageViewCompat.setImageTintList(btnWishlist, ColorStateList.valueOf(
                ContextCompat.getColor(this, wished ? R.color.heart : R.color.text_primary)));
    }

    private void share() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, "Check out the " + product.name + " on Zelora for " + Money.peso(product.price) + "!");
        startActivity(Intent.createChooser(send, "Share product"));
    }

    private boolean inStock() {
        if (product.stock <= 0) {
            UiUtil.toast(this, "Sorry, this item is out of stock.");
            return false;
        }
        return true;
    }

    private void addToCart() {
        if (!inStock()) return;
        try {
            cartDao.add(userId, product.id, selectedColor, quantity);
            UiUtil.toast(this, "Added to cart");
        } catch (Exception e) {
            UiUtil.toast(this, "Could not add to cart. Please try again.");
        }
    }

    private void buyNow() {
        if (!inStock()) return;
        ArrayList<CartItem> single = new ArrayList<>();
        single.add(new CartItem(0, product, selectedColor, quantity));
        CheckoutBottomSheet.newInstance(single).show(getSupportFragmentManager(), "checkout");
    }

    @Override
    public void onOrderPlaced() {
        finish();
    }
}
