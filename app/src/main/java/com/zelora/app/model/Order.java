package com.zelora.app.model;

import java.util.ArrayList;
import java.util.List;

// order
public class Order {
    public static final String PROCESSING = "Processing", PREPARING = "Preparing", PACKED = "Packed", OUT = "Out for Delivery",
            DELIVERED = "Delivered", CANCELLED = "Cancelled";

    public static class Line {
        public final int productId;
        public final String name;
        public final String color;
        public final int quantity;
        public final int unitPrice;

        public Line(int productId, String name, String color, int quantity, int unitPrice) {
            this.productId = productId;
            this.name = name;
            this.color = color;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }

    public int id;
    public String code;
    public int subtotal;
    public int shipping;
    public int discount;
    public String voucherCode;
    public int total;
    public String status;
    public String createdAt;
    public String recipient;
    public String address;
    public String paymentMethod;
    public String paymentStatus;
    public int customerId;
    public String customerName;
    public String deliveredAt;
    public boolean pickedUp;
    public int riderId;
    public String riderName;
    public int disputeId;
    public String disputeStatus;
    public String disputeReason;
    public String disputeNote;
    public final List<Line> lines = new ArrayList<>();

    public boolean hasOpenDispute() {
        return "Open".equals(disputeStatus);
    }
}
