package com.zelora.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zelora.app.R;
import com.zelora.app.db.AddressDao;
import com.zelora.app.db.OrderDao;
import com.zelora.app.db.UserDao;
import com.zelora.app.db.VoucherDao;
import com.zelora.app.model.Voucher;
import com.zelora.app.model.Address;
import com.zelora.app.model.CartItem;
import com.zelora.app.model.User;
import com.zelora.app.util.Money;
import com.zelora.app.util.SessionManager;
import com.zelora.app.util.Validator;

import java.util.ArrayList;
import java.util.List;

// ui
public class CheckoutBottomSheet extends BottomSheetDialogFragment {

    public interface OnOrderPlacedListener {
        void onOrderPlaced();
    }

    private static final String ARG_ITEMS = "items";
    private static final int PAY_CARD = 0, PAY_WALLET = 1, PAY_COD = 2;
    private static final String[] PAY_LABELS = {"Credit / Debit Card", "GCash / Maya", "Cash on Delivery"};

    public static CheckoutBottomSheet newInstance(ArrayList<CartItem> items) {
        CheckoutBottomSheet sheet = new CheckoutBottomSheet();
        Bundle b = new Bundle();
        b.putSerializable(ARG_ITEMS, items);
        sheet.setArguments(b);
        return sheet;
    }

    private List<CartItem> items = new ArrayList<>();
    private int userId;
    private int payment = PAY_CARD;
    private int subtotal, shipping, discount;
    private String voucherCode = null;
    private Address address;
    private AddressDao addressDao;
    private TextView tvRecipient, tvAddress, btnPlace;
    private View view;

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        view = inflater.inflate(R.layout.sheet_checkout, container, false);

        Object arg = requireArguments().getSerializable(ARG_ITEMS);
        if (arg instanceof List) items = (List<CartItem>) arg;
        userId = new SessionManager(requireContext()).getUserId();
        addressDao = new AddressDao(requireContext());

        tvRecipient = view.findViewById(R.id.tvRecipient);
        tvAddress = view.findViewById(R.id.tvAddress);
        btnPlace = view.findViewById(R.id.btnPlaceOrder);

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());
        fillItems();
        fillTotals();
        loadAddress();
        view.findViewById(R.id.cardAddress).setOnClickListener(v -> showAddressDialog());
        view.findViewById(R.id.btnChangeAddress).setOnClickListener(v -> showAddressDialog());

        view.findViewById(R.id.btnVoucher).setOnClickListener(v -> onVoucherTapped());
        view.findViewById(R.id.payCard).setOnClickListener(v -> selectPayment(PAY_CARD));
        view.findViewById(R.id.payWallet).setOnClickListener(v -> selectPayment(PAY_WALLET));
        view.findViewById(R.id.payCod).setOnClickListener(v -> selectPayment(PAY_COD));
        selectPayment(PAY_CARD);

        btnPlace.setOnClickListener(v -> onPlaceOrder());
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d instanceof BottomSheetDialog) {
            BottomSheetBehavior<FrameLayout> behavior = ((BottomSheetDialog) d).getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    // order
    private void fillItems() {
        LinearLayout box = view.findViewById(R.id.itemsContainer);
        box.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (CartItem item : items) {
            View row = inflater.inflate(R.layout.item_checkout_line, box, false);
            ((ImageView) row.findViewById(R.id.ivThumb)).setImageResource(item.product.thumbRes(requireContext()));
            ((TextView) row.findViewById(R.id.tvName)).setText(item.product.name);
            ((TextView) row.findViewById(R.id.tvColor)).setText(item.color);
            ((TextView) row.findViewById(R.id.tvPrice)).setText(Money.peso(item.lineTotal()));

            String label = "Quantity  " + item.quantity;
            SpannableString s = new SpannableString(label);
            s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), label.length() - String.valueOf(item.quantity).length(),
                    label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((TextView) row.findViewById(R.id.tvQty)).setText(s);
            box.addView(row);
        }
    }

    private void fillTotals() {
        subtotal = 0;
        for (CartItem i : items) subtotal += i.lineTotal();
        shipping = Money.shippingFor(subtotal);
        ((TextView) view.findViewById(R.id.tvSubtotal)).setText(Money.peso(subtotal));
        ((TextView) view.findViewById(R.id.tvShipping)).setText(shipping == 0 ? "Free" : Money.peso(shipping));
        showTotals();
    }

    private void showTotals() {
        view.findViewById(R.id.rowDiscount).setVisibility(voucherCode == null ? View.GONE : View.VISIBLE);
        if (voucherCode != null) {
            ((TextView) view.findViewById(R.id.tvDiscountLabel)).setText("Voucher " + voucherCode);
            ((TextView) view.findViewById(R.id.tvDiscount)).setText("-" + Money.peso(discount));
        }
        ((TextView) view.findViewById(R.id.btnVoucher)).setText(voucherCode == null
                ? "Have a voucher code?" : "Remove voucher");
        ((TextView) view.findViewById(R.id.tvTotal)).setText(Money.peso(subtotal + shipping - discount));
    }

    // voucher
    private void onVoucherTapped() {
        if (voucherCode != null) {
            voucherCode = null;
            discount = 0;
            showTotals();
            return;
        }
        View form = getLayoutInflater().inflate(R.layout.dialog_voucher, null);
        final EditText et = form.findViewById(R.id.etVoucher);
        StringBuilder hint = new StringBuilder("Available codes:");
        VoucherDao dao = new VoucherDao(requireContext());
        for (Voucher v : dao.getActive()) hint.append("\n• ").append(v.code).append(" - ").append(v.description);
        ((TextView) form.findViewById(R.id.tvVoucherHint)).setText(hint.toString());
        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Voucher")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String code = et.getText().toString().trim();
            if (code.isEmpty()) { et.setError("Enter a voucher code"); return; }
            VoucherDao.Result r = dao.check(userId, code, subtotal, shipping);
            if (r.voucher == null) { et.setError(r.error); return; }
            voucherCode = r.voucher.code;
            discount = r.discount;
            showTotals();
            dialog.dismiss();
        });
    }

    // address
    private void loadAddress() {
        address = addressDao.get(userId);
        if (address == null) {
            tvRecipient.setText("No shipping address yet");
            tvAddress.setText("Tap to add where we should deliver");
        } else {
            tvRecipient.setText(address.recipient);
            tvAddress.setText(address.line);
        }
    }

    private void showAddressDialog() {
        View form = getLayoutInflater().inflate(R.layout.dialog_address, null);
        final EditText etRecipient = form.findViewById(R.id.etRecipient);
        final EditText etAddress = form.findViewById(R.id.etAddress);
        if (address != null) {
            etRecipient.setText(address.recipient);
            etAddress.setText(address.line);
        } else {
            User u = new UserDao(requireContext()).getById(userId);
            if (u != null) etRecipient.setText(u.fullName);
        }
        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Shipping address")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String e = Validator.name(etRecipient.getText().toString());
            if (e != null) { etRecipient.setError(e); return; }
            e = Validator.address(etAddress.getText().toString());
            if (e != null) { etAddress.setError(e); return; }
            addressDao.save(userId, etRecipient.getText().toString(), etAddress.getText().toString());
            loadAddress();
            dialog.dismiss();
        });
    }

    // payment
    private void selectPayment(int which) {
        payment = which;
        style(R.id.payCard, R.id.payCardRadio, R.id.payCardTile, R.id.payCardIcon, which == PAY_CARD);
        style(R.id.payWallet, R.id.payWalletRadio, R.id.payWalletTile, R.id.payWalletIcon, which == PAY_WALLET);
        style(R.id.payCod, R.id.payCodRadio, R.id.payCodTile, R.id.payCodIcon, which == PAY_COD);
    }

    private void style(int row, int radio, int tile, int icon, boolean selected) {
        view.findViewById(row).setBackgroundResource(selected ? R.drawable.bg_pay_on : R.drawable.bg_pay_off);
        ((ImageView) view.findViewById(radio)).setImageResource(selected ? R.drawable.radio_on : R.drawable.radio_off);
        view.findViewById(tile).setBackgroundResource(selected ? R.drawable.bg_icon_tile : R.drawable.bg_icon_tile_soft);
        ImageViewCompat.setImageTintList((ImageView) view.findViewById(icon), ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), selected ? R.color.pay_selected : R.color.text_sheet_muted)));
    }

    // order
    private void onPlaceOrder() {
        if (items.isEmpty()) {
            UiUtil.toast(requireContext(), "Your order is empty.");
            return;
        }
        if (address == null) {
            UiUtil.toast(requireContext(), "Please add a shipping address first.");
            showAddressDialog();
            return;
        }
        if (payment == PAY_CARD) askCardDetails();
        else if (payment == PAY_WALLET) askWalletNumber();
        else submitOrder("PENDING");
    }

    private void askCardDetails() {
        View form = getLayoutInflater().inflate(R.layout.dialog_card, null);
        final EditText number = form.findViewById(R.id.etCardNumber);
        final EditText expiry = form.findViewById(R.id.etExpiry);
        final EditText cvv = form.findViewById(R.id.etCvv);
        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Card details")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Pay", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String e = Validator.cardNumber(number.getText().toString());
            if (e != null) { number.setError(e); return; }
            e = Validator.expiry(expiry.getText().toString());
            if (e != null) { expiry.setError(e); return; }
            e = Validator.cvv(cvv.getText().toString());
            if (e != null) { cvv.setError(e); return; }
            dialog.dismiss();
            submitOrder("PAID");
        });
    }

    private void askWalletNumber() {
        View form = getLayoutInflater().inflate(R.layout.dialog_wallet, null);
        final EditText number = form.findViewById(R.id.etWalletNumber);
        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("GCash / Maya number")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Pay", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String e = Validator.phone(number.getText().toString(), true);
            if (e != null) { number.setError(e); return; }
            dialog.dismiss();
            submitOrder("PAID");
        });
    }

    private void submitOrder(String paymentStatus) {
        btnPlace.setEnabled(false);
        final Activity activity = requireActivity();
        try {
            String code = new OrderDao(activity).placeOrder(userId, items, address.recipient, address.line,
                    PAY_LABELS[payment], paymentStatus, subtotal, shipping, voucherCode);
            dismiss();
            new MaterialAlertDialogBuilder(activity)
                    .setTitle("Order placed!")
                    .setMessage("Thank you! Your order code is " + code + ".\nTotal: " + Money.peso(subtotal + shipping - discount)
                            + "\nYou can follow it under My Orders.")
                    .setPositiveButton("OK", null)
                    .setOnDismissListener(d -> {
                        if (activity instanceof OnOrderPlacedListener) ((OnOrderPlacedListener) activity).onOrderPlaced();
                    })
                    .show();
        } catch (IllegalStateException outOfStock) {
            btnPlace.setEnabled(true);
            UiUtil.toast(activity, outOfStock.getMessage());
        } catch (Exception e) {
            btnPlace.setEnabled(true);
            UiUtil.toast(activity, "Could not place the order. Please try again.");
        }
    }
}
