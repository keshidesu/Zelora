package com.zelora.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zelora.app.R;
import com.zelora.app.db.CartDao;
import com.zelora.app.model.CartItem;
import com.zelora.app.util.Money;
import com.zelora.app.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

// ui
public class CartFragment extends Fragment implements CartAdapter.Listener {
    private CartDao cartDao;
    private CartAdapter adapter;
    private View content, empty;
    private TextView tvSubtotal, tvShipping, tvTotal;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_cart, container, false);
        userId = new SessionManager(requireContext()).getUserId();
        cartDao = new CartDao(requireContext());

        content = v.findViewById(R.id.contentGroup);
        empty = v.findViewById(R.id.emptyState);
        tvSubtotal = v.findViewById(R.id.tvSubtotal);
        tvShipping = v.findViewById(R.id.tvShipping);
        tvTotal = v.findViewById(R.id.tvTotal);

        RecyclerView rv = v.findViewById(R.id.rvCart);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CartAdapter(requireContext(), this);
        rv.setAdapter(adapter);

        v.findViewById(R.id.btnShop).setOnClickListener(x -> ((MainActivity) requireActivity()).selectTab(R.id.nav_home));
        v.findViewById(R.id.btnCheckout).setOnClickListener(x -> {
            if (adapter.getItems().isEmpty()) {
                UiUtil.toast(requireContext(), "Your cart is empty.");
                return;
            }
            CheckoutBottomSheet.newInstance(new ArrayList<>(adapter.getItems()))
                    .show(getParentFragmentManager(), "checkout");
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.submit(cartDao.getItems(userId));
        updateTotals();
    }

    @Override
    public void onCartChanged() {
        updateTotals();
        ((MainActivity) requireActivity()).refreshBadges();
    }

    private void updateTotals() {
        List<CartItem> items = adapter.getItems();
        int subtotal = 0;
        for (CartItem i : items) subtotal += i.lineTotal();
        int shipping = Money.shippingFor(subtotal);
        tvSubtotal.setText(Money.peso(subtotal));
        tvShipping.setText(shipping == 0 ? "Free" : Money.peso(shipping));
        tvTotal.setText(Money.peso(subtotal + shipping));
        content.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
