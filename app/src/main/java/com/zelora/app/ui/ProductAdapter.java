package com.zelora.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.zelora.app.R;
import com.zelora.app.db.WishlistDao;
import com.zelora.app.model.Product;
import com.zelora.app.util.Money;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// product
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface Listener {
        void onProductClick(Product product);
        void onWishlistChanged();
    }

    private static final boolean SHOW_ORIGINAL_PRICE_ON_CARD = true;

    private final Context context;
    private final int userId;
    private final WishlistDao wishlistDao;
    private final Listener listener;
    private List<Product> items = new ArrayList<>();
    private Set<Integer> wished = new HashSet<>();

    public ProductAdapter(Context context, int userId, Listener listener) {
        this.context = context;
        this.userId = userId;
        this.listener = listener;
        this.wishlistDao = new WishlistDao(context);
    }

    public void submit(List<Product> products) {
        items = products;
        wished = wishlistDao.getProductIds(userId);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context).inflate(R.layout.item_product, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Product p = items.get(position);
        h.image.setImageResource(p.thumbRes(context));
        h.name.setText(p.name);
        h.rating.setText(String.format(Locale.US, "%.1f", p.rating));
        h.reviews.setText("(" + p.reviewCount + ")");
        if (p.hasDiscount() && SHOW_ORIGINAL_PRICE_ON_CARD) {
            h.price.setText(Money.peso(p.originalPrice));
            h.oldPrice.setVisibility(View.GONE);
        } else {
            h.price.setText(Money.peso(p.price));
            if (p.hasDiscount()) {
                h.oldPrice.setVisibility(View.VISIBLE);
                h.oldPrice.setText(Money.peso(p.originalPrice));
                h.oldPrice.setPaintFlags(h.oldPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                h.oldPrice.setVisibility(View.GONE);
            }
        }

        if (p.badge.isEmpty()) {
            h.badge.setVisibility(View.GONE);
        } else {
            h.badge.setVisibility(View.VISIBLE);
            h.badge.setText(p.badge);
        }

        boolean isWished = wished.contains(p.id);
        h.heart.setImageResource(isWished ? R.drawable.ic_heart_filled : R.drawable.ic_heart);

        h.itemView.setOnClickListener(v -> listener.onProductClick(p));
        h.heartButton.setOnClickListener(v -> {
            boolean nowWished = wishlistDao.toggle(userId, p.id);
            if (nowWished) wished.add(p.id); else wished.remove(p.id);
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos);
            UiUtil.toast(context, nowWished ? "Added to wishlist" : "Removed from wishlist");
            listener.onWishlistChanged();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ShapeableImageView image;
        final TextView name, rating, reviews, price, oldPrice, badge;
        final ImageView heart;
        final View heartButton;

        VH(@NonNull View v) {
            super(v);
            image = v.findViewById(R.id.ivImage);
            name = v.findViewById(R.id.tvName);
            rating = v.findViewById(R.id.tvRating);
            reviews = v.findViewById(R.id.tvReviews);
            price = v.findViewById(R.id.tvPrice);
            oldPrice = v.findViewById(R.id.tvOldPrice);
            badge = v.findViewById(R.id.tvBadge);
            heart = v.findViewById(R.id.ivHeart);
            heartButton = v.findViewById(R.id.btnHeart);
        }
    }
}
