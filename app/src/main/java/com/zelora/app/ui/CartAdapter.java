package com.zelora.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.zelora.app.R;
import com.zelora.app.db.CartDao;
import com.zelora.app.model.CartItem;
import com.zelora.app.util.Money;

import java.util.ArrayList;
import java.util.List;

// ui
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface Listener {
        void onCartChanged();
    }

    private final Context context;
    private final CartDao cartDao;
    private final Listener listener;
    private List<CartItem> items = new ArrayList<>();

    public CartAdapter(Context context, Listener listener) {
        this.context = context;
        this.cartDao = new CartDao(context);
        this.listener = listener;
    }

    public void submit(List<CartItem> list) {
        items = list;
        notifyDataSetChanged();
    }

    public List<CartItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final CartItem item = items.get(position);
        h.thumb.setImageResource(item.product.thumbRes(context));
        h.name.setText(item.product.name);
        h.color.setText("Color: " + item.color);
        h.price.setText(Money.peso(item.lineTotal()));
        h.qty.setText(String.valueOf(item.quantity));

        final int maxQty = Math.min(CartDao.MAX_QTY, item.product.stock);

        h.minus.setOnClickListener(v -> {
            if (item.quantity > 1) changeQuantity(h, item, item.quantity - 1);
        });
        h.plus.setOnClickListener(v -> {
            if (item.quantity < maxQty) changeQuantity(h, item, item.quantity + 1);
            else UiUtil.toast(context, "Maximum quantity reached.");
        });
        h.remove.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            cartDao.remove(item.cartId);
            items.remove(pos);
            notifyItemRemoved(pos);
            listener.onCartChanged();
        });
    }

    private void changeQuantity(VH h, CartItem item, int newQty) {
        cartDao.updateQuantity(item.cartId, newQty);
        item.quantity = newQty;
        int pos = h.getBindingAdapterPosition();
        if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos);
        listener.onCartChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ShapeableImageView thumb;
        final TextView name, color, price, qty, minus, plus;
        final View remove;

        VH(@NonNull View v) {
            super(v);
            thumb = v.findViewById(R.id.ivThumb);
            name = v.findViewById(R.id.tvName);
            color = v.findViewById(R.id.tvColor);
            price = v.findViewById(R.id.tvPrice);
            qty = v.findViewById(R.id.tvQty);
            minus = v.findViewById(R.id.btnMinus);
            plus = v.findViewById(R.id.btnPlus);
            remove = v.findViewById(R.id.btnRemove);
        }
    }
}
