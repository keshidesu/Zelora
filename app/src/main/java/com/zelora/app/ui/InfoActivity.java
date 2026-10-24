package com.zelora.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.zelora.app.R;
import com.zelora.app.util.Money;

// ui
public class InfoActivity extends AppCompatActivity {
    public static final int TYPE_HELP = 0, TYPE_TERMS = 1, TYPE_PRIVACY = 2;
    private static final String EXTRA_TYPE = "type";

    public static Intent intent(Context c, int type) {
        return new Intent(c, InfoActivity.class).putExtra(EXTRA_TYPE, type);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        int type = getIntent().getIntExtra(EXTRA_TYPE, TYPE_HELP);
        String title, body;
        switch (type) {
            case TYPE_TERMS:
                title = "Terms & Conditions";
                body = "1. Using Zelora\nBy creating an account you confirm that the information you give is true and that you will keep your password private.\n\n"
                        + "2. Orders and prices\nAll prices are in Philippine pesos (₱). An order is confirmed when you tap Place Order and you receive an order code.\n\n"
                        + "3. Shipping\nShipping is ₱" + Money.SHIPPING_FEE + " per order. Orders of " + Money.peso(Money.FREE_SHIPPING_MIN)
                        + " and above ship for free.\n\n"
                        + "4. Payments\nCash on Delivery orders are paid when the parcel arrives. Card and e-wallet payments in this version are simulated for demonstration.\n\n"
                        + "5. Changes\nThese terms may be updated as the app grows.";
                break;
            case TYPE_PRIVACY:
                title = "Privacy Policy";
                body = "Zelora stores your name, email, address and orders only on this device, in a local database.\n\n"
                        + "Your password is never saved as text: only a salted hash is stored.\n\n"
                        + "Card numbers entered at checkout are checked and then discarded; they are never saved.";
                break;
            default:
                title = "Help Center";
                body = "How do I place an order?\nOpen a product, choose a color and quantity, then tap Buy Now (or Add to Cart and open the Cart tab).\n\n"
                        + "How do I change my address?\nOpen Account Settings, or tap Change on the Checkout sheet.\n\n"
                        + "How much is shipping?\n₱" + Money.SHIPPING_FEE + ", and free for orders of " + Money.peso(Money.FREE_SHIPPING_MIN) + " and above.\n\n"
                        + "Where can I see my orders?\nOpen the side menu and tap My Orders.\n\n"
                        + "Still need help?\nEmail support@zelora.example";
        }
        UiUtil.setupToolbar(this, title);
        ((TextView) findViewById(R.id.tvBody)).setText(body);
    }
}
