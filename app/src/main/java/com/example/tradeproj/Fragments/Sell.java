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

public class Sell extends Fragment {
    private EditText stockSymbolEditText, quantityEditText, priceEditText;
    private Button sellButton, backButton;
    private FirebaseManager firebaseManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sell, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stockSymbolEditText = view.findViewById(R.id.stockSymbolEditText);
        quantityEditText = view.findViewById(R.id.quantityEditText);
        priceEditText = view.findViewById(R.id.priceEditText);
        sellButton = view.findViewById(R.id.sellButton);
        backButton = view.findViewById(R.id.backBt);
        firebaseManager = FirebaseManager.getInstance();

        // ✅ Auto-fill stock symbol if navigated from TradeFragment
        if (getArguments() != null) {
            String selectedStock = getArguments().getString("selectedStock", "");
            stockSymbolEditText.setText(selectedStock);
        }

        sellButton.setOnClickListener(v -> sellStock());
        backButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.trade));
    }

    private void sellStock() {
        String symbol = stockSymbolEditText.getText().toString().trim();
        int quantity;
        double price;

        try {
            quantity = Integer.parseInt(quantityEditText.getText().toString().trim());
            price = Double.parseDouble(priceEditText.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid input values", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseManager.sellStock(symbol, quantity, price, () -> {
            Toast.makeText(getContext(), "Stock Sold!", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigate(R.id.trade);
        }, () -> Toast.makeText(getContext(), "Not enough shares to sell!", Toast.LENGTH_SHORT).show());
    }

}
