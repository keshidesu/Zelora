package com.zelora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zelora.app.R;
import com.zelora.app.db.UserDao;
import com.zelora.app.model.User;
import com.zelora.app.util.SessionManager;

// ui
public class AccountFragment extends Fragment {
    private TextView tvAvatar, tvName, tvEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_account, container, false);
        tvAvatar = v.findViewById(R.id.tvAvatar);
        tvName = v.findViewById(R.id.tvName);
        tvEmail = v.findViewById(R.id.tvEmail);

        v.findViewById(R.id.rowOrders).setOnClickListener(x -> startActivity(new Intent(requireContext(), OrdersActivity.class)));
        v.findViewById(R.id.rowSettings).setOnClickListener(x -> startActivity(new Intent(requireContext(), AccountSettingsActivity.class)));
        v.findViewById(R.id.rowHelp).setOnClickListener(x -> startActivity(InfoActivity.intent(requireContext(), InfoActivity.TYPE_HELP)));
        v.findViewById(R.id.rowTerms).setOnClickListener(x -> startActivity(InfoActivity.intent(requireContext(), InfoActivity.TYPE_TERMS)));
        v.findViewById(R.id.btnLogout).setOnClickListener(x -> ((MainActivity) requireActivity()).confirmLogout());
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        User u = new UserDao(requireContext()).getById(new SessionManager(requireContext()).getUserId());
        if (u != null) {
            tvAvatar.setText(u.initials());
            tvName.setText(u.fullName);
            tvEmail.setText(u.email);
        }
    }
}
