package com.example.tradeproj.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
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
import java.util.Collections;

public class Trade extends Fragment {
    private RecyclerView stocksRecyclerView;
    private StocksAdapter stockAdapter;
    private StockViewModel stockViewModel;
    private SearchView searchView;
    private Button portfolioButton;
    private FinnhubApi finnhubApi;
    private FirebaseManager firebaseManager;
    private String lastSearchQuery = "";

    private List<StockItem> cachedTopStocks = new ArrayList<>(); // ✅ Stores the fixed top 10 stocks

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
        finnhubApi = FinnhubApi.getInstance(requireContext());

        stockAdapter = new StocksAdapter(new ArrayList<>(), getContext(), stock -> showBuySellDialog(view, stock));
        stocksRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        stocksRecyclerView.setAdapter(stockAdapter);

        stockViewModel = new ViewModelProvider(this).get(StockViewModel.class);
        stockViewModel.getStockList().observe(getViewLifecycleOwner(), this::updateStockList);

        fetchTopStocks(); // ✅ Load top stocks ONCE

        portfolioButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.portfolio));

        // ✅ Handle Search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.trim().isEmpty()) {
                    lastSearchQuery = query.toUpperCase().trim();
                    fetchStockPrices(Collections.singletonList(lastSearchQuery)); // ✅ Use FinnhubApi function
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                lastSearchQuery = newText.trim();
                if (newText.isEmpty()) {
                    restoreTopStocks(); // ✅ Restore SAME top 10 stocks
                } else {
                    filterStockList(newText);
                }
                return true;
            }
        });
    }

    /**
     * ✅ Ensures top 10 stocks are only fetched once and stored.
     */
    private void fetchTopStocks() {
        if (!cachedTopStocks.isEmpty()) {
            Log.d(TAG, "✅ Using cached top 10 stocks.");
            updateStockList(cachedTopStocks);
            return;
        }

        Log.d(TAG, "🔍 Fetching new top 10 stocks...");
        finnhubApi.fetchTopActiveStocks(symbols -> {
            if (symbols != null && !symbols.isEmpty()) {
                finnhubApi.fetchStockPrices(symbols,
                        stockItems -> requireActivity().runOnUiThread(() -> {
                            cachedTopStocks = new ArrayList<>(stockItems); // ✅ Cache top stocks
                            updateStockList(stockItems);
                        }),
                        error -> Log.e(TAG, "❌ Error fetching top stock prices: " + error)
                );
            }
        });
    }

    /**
     * ✅ Fetches stock prices for searched stock.
     */
    private void fetchStockPrices(List<String> symbols) {
        finnhubApi.fetchStockPrices(symbols,
                stockItems -> requireActivity().runOnUiThread(() -> updateStockList(stockItems)),
                error -> Log.e(TAG, "❌ Error fetching stock prices: " + error)
        );
    }

    /**
     * ✅ Updates stock list while caching top 10 stocks.
     */
    private void updateStockList(List<StockItem> stockItems) {
        if (cachedTopStocks.isEmpty()) {
            cachedTopStocks = new ArrayList<>(stockItems);
        }

        firebaseManager.getUserFavorites().thenAccept(favoriteSymbols -> {
            for (StockItem stock : stockItems) {
                stock.setFavorite(favoriteSymbols.contains(stock.getSymbol())); // ✅ Update favorite status
            }
            requireActivity().runOnUiThread(() -> stockAdapter.updateStocks(stockItems));
        });
    }

    /**
     * ✅ Restores the **same** top 10 stocks when clearing the search.
     */
    private void restoreTopStocks() {
        Log.d(TAG, "🔄 Restoring top 10 stocks...");
        updateStockList(cachedTopStocks);
    }

    /**
     * ✅ Filters the list dynamically while typing.
     */
    private void filterStockList(String query) {
        List<StockItem> filteredList = new ArrayList<>();
        for (StockItem stock : cachedTopStocks) {
            if (stock.getSymbol().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(stock);
            }
        }
        updateStockList(filteredList);
    }

    /**
     * ✅ Displays Buy/Sell dialog.
     */
    private void showBuySellDialog(View parentView, StockItem stock) {
        new AlertDialog.Builder(getContext())
                .setTitle("Choose an action")
                .setMessage("Do you want to buy or sell " + stock.getSymbol() + "?")
                .setPositiveButton("Buy", (dialog, which) -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("selectedStock", stock.getSymbol());
                    Navigation.findNavController(parentView).navigate(R.id.buy, bundle);
                })
                .setNegativeButton("Sell", (dialog, which) -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("selectedStock", stock.getSymbol());
                    Navigation.findNavController(parentView).navigate(R.id.sell, bundle);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "📢 Resuming Trade Page...");

        // ✅ Fetch Top 10 Active Stocks from FinnhubApi
        finnhubApi.fetchTopActiveStocks(symbols -> {
            if (symbols != null && !symbols.isEmpty()) {
                finnhubApi.fetchStockPrices(symbols,
                        stockItems -> requireActivity().runOnUiThread(() -> stockAdapter.updateStocks(stockItems)),
                        error -> Log.e(TAG, "❌ Error fetching top stock prices: " + error)
                );
            }
        });
    }
}
