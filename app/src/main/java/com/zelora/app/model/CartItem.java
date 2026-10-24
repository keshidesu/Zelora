package com.zelora.app.model;

import java.io.Serializable;

// cart
public class CartItem implements Serializable {
    public int cartId;
    public final Product product;
    public final String color;
    public int quantity;

    public CartItem(int cartId, Product product, String color, int quantity) {
        this.cartId = cartId;
        this.product = product;
        this.color = color;
        this.quantity = quantity;
    }

    public int lineTotal() {
        return product.price * quantity;
    }
}
