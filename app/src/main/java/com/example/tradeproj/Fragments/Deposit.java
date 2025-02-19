package com.example.tradeproj.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.tradeproj.R;
import com.example.tradeproj.handlers.FirebaseManager;


public class Deposit extends Fragment {
    private EditText amountEditText;
    private Button depositButton, backButton;
    private FirebaseManager firebaseManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_deposit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        amountEditText = view.findViewById(R.id.amountET);
        depositButton = view.findViewById(R.id.depositBtn);
        backButton = view.findViewById(R.id.backDeposit);
        firebaseManager = FirebaseManager.getInstance();

        depositButton.setOnClickListener(v -> depositCash());
        backButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.portfolio));
    }

    private void depositCash() {
        double amount;
        try {
            amount = Double.parseDouble(amountEditText.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid amount!", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseManager.depositCash(amount, () -> {
            Toast.makeText(getContext(), "Cash Deposited!", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigate(R.id.portfolio);
        }, () -> Toast.makeText(getContext(), "Error Depositing Cash!", Toast.LENGTH_SHORT).show());
    }
}
