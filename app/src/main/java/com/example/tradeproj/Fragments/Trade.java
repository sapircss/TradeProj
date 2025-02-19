package com.example.tradeproj.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tradeproj.Data.StocksAdapter;
import com.example.tradeproj.Models.StockViewModel;
import com.example.tradeproj.R;
import com.example.tradeproj.handlers.FinnhubApi;
import com.example.tradeproj.handlers.FirebaseManager;
import com.example.tradeproj.items.StockItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Collections;

public class Trade extends Fragment {
    private RecyclerView stocksRecyclerView;
    private StocksAdapter stockAdapter;
    private StockViewModel stockViewModel;
    private SearchView searchView;
    private Button portfolioButton;
    private FinnhubApi finnhubApi;
    private FirebaseManager firebaseManager;
    private boolean isFiltered = false;

    private static final String TAG = "TradeFragment";

    public Trade() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trade, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stocksRecyclerView = view.findViewById(R.id.stocks_recycler_view);
        searchView = view.findViewById(R.id.search_stock);
        portfolioButton = view.findViewById(R.id.portfolioButton);

        firebaseManager = FirebaseManager.getInstance();
        stockAdapter = new StocksAdapter(new ArrayList<>(), getContext(), stock -> showBuySellDialog(view, stock));
        stocksRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        stocksRecyclerView.setAdapter(stockAdapter);

        // 🔹 FIX: Pass context to FinnhubApi to prevent getInstance() error
        finnhubApi = FinnhubApi.getInstance(requireContext());

        stockViewModel = new ViewModelProvider(this).get(StockViewModel.class);
        stockViewModel.getStockList().observe(getViewLifecycleOwner(), this::updateStockList);

        fetchTopStocks();

        portfolioButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.portfolio));

        checkForFavoriteStockUpdates();
    }

    private void fetchTopStocks() {
        stockViewModel.fetchTopStocks();
    }

    private void fetchStockData(List<String> symbols) {
        stockViewModel.fetchStockData(symbols);
    }

    private void updateStockList(List<StockItem> stockItems) {
        stockAdapter.updateStocks(stockItems);
    }


    private void checkForFavoriteStockUpdates() {
        firebaseManager.getUserFavorites().thenAccept((List<String> favoriteSymbols) -> {
            // Ensure favoriteSymbols is not null
            favoriteSymbols = Objects.requireNonNullElseGet(favoriteSymbols, Collections::emptyList);

            if (favoriteSymbols.isEmpty()) {
                Log.d(TAG, "⚠ No favorite stocks to check updates.");
                return;
            }

            // ✅ FIXED: Now passing both success and error callbacks
            finnhubApi.fetchStockPrices(favoriteSymbols,
                    updatedStocks -> { // Success Callback
                        updatedStocks = Objects.requireNonNullElseGet(updatedStocks, Collections::emptyList);

                        if (updatedStocks.isEmpty()) {
                            Log.d(TAG, "⚠ No updates for favorite stocks.");
                            return;
                        }

                        for (StockItem stock : updatedStocks) {
                            if (Math.abs(stock.getPriceChangePercentage()) > 1.0) {
                                showNotification(stock);
                            }
                        }
                    },
                    errorMessage -> { // Error Callback
                        Log.e(TAG, "❌ Error fetching stock updates: " + errorMessage);
                    }
            );
        }).exceptionally(e -> {
            Log.e(TAG, "❌ Error fetching favorite stock updates", e);
            return null;
        });
    }

    private void showBuySellDialog(View view, StockItem stock) {
        new AlertDialog.Builder(getContext())
                .setTitle("Choose an action")
                .setMessage("Do you want to buy or sell " + stock.getSymbol() + "?")
                .setPositiveButton("Buy", (dialog, which) -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("selectedStock", stock.getSymbol());
                    Navigation.findNavController(view).navigate(R.id.buy, bundle);
                })
                .setNegativeButton("Sell", (dialog, which) -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("selectedStock", stock.getSymbol());
                    Navigation.findNavController(view).navigate(R.id.sell, bundle);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }



    private void showNotification(StockItem stock) {
        new AlertDialog.Builder(getContext())
                .setTitle("Stock Price Alert")
                .setMessage(stock.getSymbol() + " changed by " + String.format("%.2f%%", stock.getPriceChangePercentage()))
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchTopStocks();
    }
}
