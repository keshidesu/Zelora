package com.zelora.app.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.zelora.app.R;
import com.zelora.app.db.UserDao;
import com.zelora.app.model.User;
import com.zelora.app.util.SessionManager;
import com.zelora.app.util.Validator;

// auth
public class SignUpActivity extends AppCompatActivity {
    private EditText etName, etEmail, etPassword, etConfirm;
    private CheckBox cbTerms;
    private String role = User.CUSTOMER;
    private static final String[] ROLES = {User.CUSTOMER, User.SELLER, User.RIDER};
    private static final String[] ROLE_LABELS = {"Customer (shop on Zelora)", "Seller (sell my products)", "Delivery rider (deliver orders)"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        cbTerms = findViewById(R.id.cbTerms);

        UiUtil.setupPasswordToggle(etPassword, (ImageView) findViewById(R.id.btnTogglePassword));
        UiUtil.setupPasswordToggle(etConfirm, (ImageView) findViewById(R.id.btnToggleConfirm));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCreate).setOnClickListener(v -> attemptSignUp());

        View.OnClickListener social = v -> UiUtil.toast(this, "Social sign-up is not available in this offline demo.");
        findViewById(R.id.btnGoogle).setOnClickListener(social);
        findViewById(R.id.btnApple).setOnClickListener(social);
        findViewById(R.id.btnFacebook).setOnClickListener(social);

        UiUtil.setLink(this, (TextView) findViewById(R.id.tvGoLogin),
                "Already have an account?  ", "Log in", this::finish);

        buildTermsText();
        drawRoleLink();
    }

    private void drawRoleLink() {
        TextView tv = findViewById(R.id.tvJoinStaff);
        if (User.CUSTOMER.equals(role)) {
            UiUtil.setLink(this, tv, "Want to sell or deliver?  ", "Join as a Seller or Rider", this::chooseRole);
        } else {
            UiUtil.setLink(this, tv, "Signing up as a " + User.roleLabel(role).toLowerCase() + ".  ", "Change", this::chooseRole);
        }
    }

    private void chooseRole() {
        int checked = 0;
        for (int i = 0; i < ROLES.length; i++) if (ROLES[i].equals(role)) checked = i;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("I want to join Zelora as")
                .setSingleChoiceItems(ROLE_LABELS, checked, (d, which) -> {
                    role = ROLES[which];
                    drawRoleLink();
                    d.dismiss();
                })
                .show();
    }

    private void buildTermsText() {
        String text = "I agree to the Terms & Conditions and Privacy Policy.";
        SpannableString s = new SpannableString(text);
        addLink(s, text, "Terms & Conditions", InfoActivity.TYPE_TERMS);
        addLink(s, text, "Privacy Policy", InfoActivity.TYPE_PRIVACY);
        TextView tv = findViewById(R.id.tvTerms);
        tv.setText(s);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setHighlightColor(0);
    }

    private void addLink(SpannableString s, String full, String word, final int type) {
        int start = full.indexOf(word);
        s.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(InfoActivity.intent(SignUpActivity.this, type));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setUnderlineText(false);
                ds.setColor(getColor(R.color.btn_primary));
            }
        }, start, start + word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new StyleSpan(Typeface.BOLD), start, start + word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void attemptSignUp() {
        String name = etName.getText().toString();
        String email = etEmail.getText().toString();
        String pass = etPassword.getText().toString();
        String confirm = etConfirm.getText().toString();

        String error;
        if ((error = Validator.name(name)) != null) { fail(etName, error); return; }
        if ((error = Validator.email(email)) != null) { fail(etEmail, error); return; }
        if ((error = Validator.password(pass)) != null) { fail(etPassword, error); return; }
        if (!pass.equals(confirm)) { fail(etConfirm, "Passwords do not match"); return; }
        if (!cbTerms.isChecked()) {
            UiUtil.toast(this, "Please agree to the Terms & Conditions to continue.");
            return;
        }

        try {
            long id = new UserDao(this).register(name, email, pass, role);
            if (id == -1) {
                fail(etEmail, "This email is already registered");
                return;
            }
            new SessionManager(this).login((int) id);
            UiUtil.toast(this, "Welcome to Zelora, " + name.trim().split("\\s+")[0] + "!");
            Nav.openHome(this, new UserDao(this).getById((int) id));
        } catch (Exception e) {
            UiUtil.toast(this, "Could not create the account. Please try again.");
        }
    }

    private void fail(EditText field, String message) {
        field.setError(message);
        field.requestFocus();
    }
}
