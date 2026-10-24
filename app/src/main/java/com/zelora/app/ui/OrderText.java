package com.zelora.app.ui;

import com.zelora.app.model.Order;
import com.zelora.app.util.Money;

// order
public final class OrderText {
    private OrderText() {}

    public static String details(Order o) {
        StringBuilder body = new StringBuilder();
        for (Order.Line l : o.lines) {
            if (body.length() > 0) body.append('\n');
            body.append("• ").append(l.quantity).append(" × ").append(l.name)
                    .append(" (").append(l.color).append(")  ").append(Money.peso(l.quantity * l.unitPrice));
        }
        body.append("\n\nShip to: ").append(o.recipient).append(", ").append(o.address);
        String pay = "PAID".equals(o.paymentStatus) ? "Paid"
                : "REFUNDED".equals(o.paymentStatus) ? "Refunded"
                : "CANCELLED".equals(o.paymentStatus) ? "Cancelled" : "Collect cash on delivery";
        body.append("\nPayment: ").append(o.paymentMethod).append(" • ").append(pay);
        if (o.discount > 0) body.append("\nVoucher ").append(o.voucherCode).append(": -").append(Money.peso(o.discount));
        if (o.riderName != null) body.append("\nRider: ").append(o.riderName);
        if (o.disputeStatus != null) {
            body.append("\nProblem report (").append(o.disputeStatus).append("): ").append(o.disputeReason);
        }
        return body.toString();
    }
}
