package com.example.tradeproj.Fragments;

import android.os.Bundle;
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
        stockAdapter = new StocksAdapter(new ArrayList<>(), getContext(), stock -> showBuySellDepositDialog(view, stock));
        stocksRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        stocksRecyclerView.setAdapter(stockAdapter);

        finnhubApi = FinnhubApi.getInstance(requireContext());
        stockViewModel = new ViewModelProvider(this).get(StockViewModel.class);
        stockViewModel.getStockList().observe(getViewLifecycleOwner(), this::updateStockList);

        fetchTopStocks();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                isFiltered = true;
                fetchStockData(List.of(query.toUpperCase()));
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty() && isFiltered) {
                    isFiltered = false;
                    fetchTopStocks();
                }
                return false;
            }
        });

        portfolioButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.portfolio));

        checkForFavoriteStockUpdates();
    }

    private void fetchTopStocks() {
        finnhubApi.fetchTopActiveStocks(symbols -> {
            if (symbols == null || symbols.isEmpty()) {
                Toast.makeText(getContext(), "Failed to fetch top stocks!", Toast.LENGTH_SHORT).show();
                return;
            }
            fetchStockData(symbols);
        });
    }

    private void fetchStockData(List<String> symbols) {
        finnhubApi.fetchStockPrices(symbols, stockItems -> {
            if (stockItems == null || stockItems.isEmpty()) {
                Toast.makeText(getContext(), "Failed to fetch stock prices!", Toast.LENGTH_SHORT).show();
                return;
            }
            updateStockList(stockItems);
        });
    }

    private void updateStockList(List<StockItem> stockItems) {
        stockAdapter.updateStocks(stockItems);
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

    private void showBuySellDepositDialog(View view, StockItem stock) {
        new AlertDialog.Builder(getContext())
                .setTitle("Choose an action")
                .setMessage("Do you want to buy, sell, or deposit cash?")
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
                .setNeutralButton("Deposit Cash", (dialog, which) ->
                        Navigation.findNavController(view).navigate(R.id.deposit))
                .setCancelable(true)
                .show();
    }

    private void checkForFavoriteStockUpdates() {
        firebaseManager.getUserFavorites().thenAccept(favoriteSymbols -> {
            finnhubApi.checkFavoriteStockUpdates(favoriteSymbols, updatedStocks -> {
                for (StockItem stock : updatedStocks) {
                    if (Math.abs(stock.getPriceChangePercentage()) > 1.0) { // Notify if price change > 1%
                        showNotification(stock);
                    }
                }
            });
        });
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
        fetchTopStocks(); // 🔹 Reload top 10 stocks when returning to Trade page
    }

}
