package com.zelora.app.ui;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.zelora.app.R;
import com.zelora.app.db.AddressDao;
import com.zelora.app.db.UserDao;
import com.zelora.app.model.Address;
import com.zelora.app.model.User;
import com.zelora.app.util.SessionManager;
import com.zelora.app.util.Validator;

// ui
public class AccountSettingsActivity extends AppCompatActivity {
    private EditText etName, etEmail, etPhone, etRecipient, etAddress, etCurrent, etNew, etConfirm;
    private UserDao userDao;
    private AddressDao addressDao;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);
        UiUtil.setupToolbar(this, "Account Settings");

        userId = new SessionManager(this).getUserId();
        userDao = new UserDao(this);
        addressDao = new AddressDao(this);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etRecipient = findViewById(R.id.etRecipient);
        etAddress = findViewById(R.id.etAddress);
        etCurrent = findViewById(R.id.etCurrent);
        etNew = findViewById(R.id.etNew);
        etConfirm = findViewById(R.id.etConfirm);

        User u = userDao.getById(userId);
        if (u != null) {
            etName.setText(u.fullName);
            etEmail.setText(u.email);
            etEmail.setEnabled(false);
            etPhone.setText(u.phone);
        }
        Address a = addressDao.get(userId);
        if (a != null) {
            etRecipient.setText(a.recipient);
            etAddress.setText(a.line);
        }

        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnSaveAddress).setOnClickListener(v -> saveAddress());
        findViewById(R.id.btnChangePassword).setOnClickListener(v -> changePassword());
    }

    private void saveProfile() {
        String e = Validator.name(etName.getText().toString());
        if (e != null) { etName.setError(e); return; }
        e = Validator.phone(etPhone.getText().toString(), false);
        if (e != null) { etPhone.setError(e); return; }
        userDao.updateProfile(userId, etName.getText().toString(), etPhone.getText().toString());
        UiUtil.hideKeyboard(this);
        UiUtil.toast(this, "Profile saved.");
    }

    private void saveAddress() {
        String e = Validator.name(etRecipient.getText().toString());
        if (e != null) { etRecipient.setError(e); return; }
        e = Validator.address(etAddress.getText().toString());
        if (e != null) { etAddress.setError(e); return; }
        addressDao.save(userId, etRecipient.getText().toString(), etAddress.getText().toString());
        UiUtil.hideKeyboard(this);
        UiUtil.toast(this, "Address saved.");
    }

    private void changePassword() {
        String current = etCurrent.getText().toString();
        String next = etNew.getText().toString();
        if (current.isEmpty()) { etCurrent.setError("Enter your current password"); return; }
        String e = Validator.password(next);
        if (e != null) { etNew.setError(e); return; }
        if (!next.equals(etConfirm.getText().toString())) { etConfirm.setError("Passwords do not match"); return; }
        if (userDao.changePassword(userId, current, next)) {
            etCurrent.setText("");
            etNew.setText("");
            etConfirm.setText("");
            UiUtil.hideKeyboard(this);
            UiUtil.toast(this, "Password updated.");
        } else {
            etCurrent.setError("Current password is incorrect");
        }
    }
}
