package com.zelora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zelora.app.R;
import com.zelora.app.db.UserDao;
import com.zelora.app.model.User;
import com.zelora.app.util.SessionManager;
import com.zelora.app.util.Validator;

// auth
public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private UserDao userDao;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        if (session.isLoggedIn()) {
            User saved = new UserDao(this).getById(session.getUserId());
            if (saved != null && saved.active) {
                Nav.openHome(this, saved);
                return;
            }
            session.logout();
        }
        setContentView(R.layout.activity_login);
        userDao = new UserDao(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        UiUtil.setupPasswordToggle(etPassword, (ImageView) findViewById(R.id.btnTogglePassword));

        findViewById(R.id.btnLogin).setOnClickListener(v -> attemptLogin());
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                attemptLogin();
                return true;
            }
            return false;
        });
        findViewById(R.id.tvForgot).setOnClickListener(v -> showForgotPassword());

        View.OnClickListener social = v -> UiUtil.toast(this, "Social sign-in is not available in this offline demo.");
        findViewById(R.id.btnGoogle).setOnClickListener(social);
        findViewById(R.id.btnApple).setOnClickListener(social);
        findViewById(R.id.btnFacebook).setOnClickListener(social);

        UiUtil.setLink(this, (android.widget.TextView) findViewById(R.id.tvGoSignUp),
                "Don't have an account?  ", "Sign Up",
                () -> startActivity(new Intent(this, SignUpActivity.class)));
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        String emailError = Validator.email(email);
        if (emailError != null) {
            etEmail.setError(emailError);
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        try {
            User user = userDao.login(email, password);
            if (user == null) {
                UiUtil.toast(this, "Incorrect email or password.");
                return;
            }
            if (!user.active) {
                UiUtil.toast(this, "This account has been disabled. Please contact the Zelora admin.");
                return;
            }
            session.login(user.id);
            UiUtil.hideKeyboard(this);
            Nav.openHome(this, user);
        } catch (Exception e) {
            UiUtil.toast(this, "Something went wrong. Please try again.");
        }
    }

    private void showForgotPassword() {
        View view = getLayoutInflater().inflate(R.layout.dialog_forgot, null);
        final EditText email = view.findViewById(R.id.etEmail);
        final EditText newPass = view.findViewById(R.id.etNewPassword);
        email.setText(etEmail.getText());

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Reset password")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String e = Validator.email(email.getText().toString());
            if (e != null) { email.setError(e); return; }
            String p = Validator.password(newPass.getText().toString());
            if (p != null) { newPass.setError(p); return; }
            if (userDao.resetPassword(email.getText().toString(), newPass.getText().toString())) {
                UiUtil.toast(this, "Password updated. You can log in now.");
                dialog.dismiss();
            } else {
                email.setError("No account found with this email");
            }
        });
    }
}
