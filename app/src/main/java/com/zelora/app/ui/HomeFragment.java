package com.zelora.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zelora.app.R;
import com.zelora.app.db.CartDao;
import com.zelora.app.db.ProductDao;
import com.zelora.app.model.Product;
import com.zelora.app.util.Money;
import com.zelora.app.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

// ui
public class HomeFragment extends Fragment implements ProductAdapter.Listener {
    private static final String[] CATEGORIES = {"All", "Bags", "Apparel", "Accessories"};
    private static final String[] SORT_LABELS = {"Featured", "Price: Low to High", "Price: High to Low", "Top Rated"};

    private ProductDao productDao;
    private ProductAdapter adapter;
    private EditText etSearch;
    private TextView tvEmpty, tvCartBadge;
    private final List<TextView> chips = new ArrayList<>();
    private String category = "All";
    private int sort = ProductDao.SORT_DEFAULT;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        userId = new SessionManager(requireContext()).getUserId();
        productDao = new ProductDao(requireContext());

        etSearch = v.findViewById(R.id.etSearch);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        tvCartBadge = v.findViewById(R.id.tvCartBadge);

        v.findViewById(R.id.btnMenu).setOnClickListener(x -> ((MainActivity) requireActivity()).openDrawer());
        v.findViewById(R.id.btnCart).setOnClickListener(x -> ((MainActivity) requireActivity()).selectTab(R.id.nav_cart));

        ((TextView) v.findViewById(R.id.tvBannerSub)).setText("On orders " + Money.peso(Money.FREE_SHIPPING_MIN) + " and above.");
        v.findViewById(R.id.bannerShipping).setOnClickListener(x -> UiUtil.toast(requireContext(),
                "Free shipping applies automatically at checkout."));
        v.findViewById(R.id.btnFilters).setOnClickListener(x -> showSortDialog());

        RecyclerView rv = v.findViewById(R.id.rvProducts);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new ProductAdapter(requireContext(), userId, this);
        rv.setAdapter(adapter);

        buildChips((LinearLayout) v.findViewById(R.id.chipContainer));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { loadProducts(); }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProducts();
        updateCartBadge();
    }

    private void loadProducts() {
        List<Product> list = productDao.query(category, etSearch.getText().toString(), sort);
        adapter.submit(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateCartBadge() {
        int count = new CartDao(requireContext()).count(userId);
        tvCartBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        tvCartBadge.setText(count > 9 ? "9+" : String.valueOf(count));
    }

    private void buildChips(LinearLayout container) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            final String name = CATEGORIES[i];
            TextView chip = new TextView(requireContext());
            chip.setText(name);
            chip.setTextSize(11);
            chip.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.inter_medium));
            chip.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) lp.setMarginStart(UiUtil.dp(requireContext(), 8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(x -> {
                category = name;
                styleChips();
                loadProducts();
            });
            chips.add(chip);
            container.addView(chip);
        }
        styleChips();
    }

    private void styleChips() {
        for (int i = 0; i < chips.size(); i++) {
            TextView chip = chips.get(i);
            boolean selected = CATEGORIES[i].equals(category);
            chip.setBackgroundResource(selected ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
            chip.setTextColor(ContextCompat.getColor(requireContext(), selected ? R.color.white : R.color.text_primary));
        }
    }

    private void showSortDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort products")
                .setSingleChoiceItems(SORT_LABELS, sort, (dialog, which) -> {
                    sort = which;
                    loadProducts();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onProductClick(Product product) {
        startActivity(ProductDetailActivity.intent(requireContext(), product.id));
    }

    @Override
    public void onWishlistChanged() {}
}
