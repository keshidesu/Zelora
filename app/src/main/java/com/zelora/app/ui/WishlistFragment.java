package com.zelora.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zelora.app.R;
import com.zelora.app.db.WishlistDao;
import com.zelora.app.model.Product;
import com.zelora.app.util.SessionManager;

import java.util.List;

// wishlist
public class WishlistFragment extends Fragment implements ProductAdapter.Listener {
    private WishlistDao wishlistDao;
    private ProductAdapter adapter;
    private View content, empty;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_wishlist, container, false);
        userId = new SessionManager(requireContext()).getUserId();
        wishlistDao = new WishlistDao(requireContext());
        content = v.findViewById(R.id.contentGroup);
        empty = v.findViewById(R.id.emptyState);

        RecyclerView rv = v.findViewById(R.id.rvWishlist);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new ProductAdapter(requireContext(), userId, this);
        rv.setAdapter(adapter);

        v.findViewById(R.id.btnShop).setOnClickListener(x -> ((MainActivity) requireActivity()).selectTab(R.id.nav_home));
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<Product> list = wishlistDao.getProducts(userId);
        adapter.submit(list);
        content.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onProductClick(Product product) {
        startActivity(ProductDetailActivity.intent(requireContext(), product.id));
    }

    @Override
    public void onWishlistChanged() {
        reload();
    }
}
