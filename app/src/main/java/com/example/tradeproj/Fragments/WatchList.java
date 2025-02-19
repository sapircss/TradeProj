package com.example.tradeproj.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tradeproj.Data.WatchListAdapter;
import com.example.tradeproj.R;
import com.example.tradeproj.items.StockItem;
import com.example.tradeproj.items.WatchListItm;
import com.example.tradeproj.handlers.FirebaseManager;
import com.example.tradeproj.handlers.FinnhubApi;
import java.util.ArrayList;
import java.util.List;

public class WatchList extends Fragment {

    private RecyclerView watchlistRecyclerView;
    private WatchListAdapter watchListAdapter;
    private FirebaseManager firebaseManager;
    private FinnhubApi finnhubApi;
    private Button backToTradeButton, backToPortfolioButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate layout
        View view = inflater.inflate(R.layout.fragment_watch_list, container, false);

        // Initialize UI components
        watchlistRecyclerView = view.findViewById(R.id.watchlist_recycler_view);
        watchlistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        backToTradeButton = view.findViewById(R.id.back_to_trade);
        backToPortfolioButton = view.findViewById(R.id.back_to_portfolio);

        firebaseManager = FirebaseManager.getInstance();
        finnhubApi = FinnhubApi.getInstance(getContext());

        // Fetch and update watchlist data
        fetchFavoriteStocks();

        // Handle navigation buttons
        backToTradeButton.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_watchlist_to_trade));
        backToPortfolioButton.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_watchlist_to_portfolio));

        return view;
    }

    private void fetchFavoriteStocks() {
        firebaseManager.getUserFavorites().thenAccept(favoriteSymbols -> {
            if (favoriteSymbols == null || favoriteSymbols.isEmpty()) {
                return; // No favorite stocks
            }
            fetchStockPrices(favoriteSymbols);
        });
    }

    private void fetchStockPrices(List<String> favoriteSymbols) {
        finnhubApi.fetchStockPrices(favoriteSymbols,
                stockItems -> { // ✅ Success Callback
                    if (stockItems != null && !stockItems.isEmpty()) {
                        List<WatchListItm> watchListItems = convertToWatchListItems(stockItems);
                        updateWatchlist(watchListItems);
                    }
                },
                error -> { // ✅ Error Callback
                    Log.e("WatchList", "❌ Error fetching stock prices: " + error);
                }
        );
    }


    private List<WatchListItm> convertToWatchListItems(List<StockItem> stockItems) {
        List<WatchListItm> watchListItems = new ArrayList<>();
        for (StockItem stock : stockItems) {
            watchListItems.add(new WatchListItm(stock.getSymbol(), stock.getPrice(), stock.getPercentChange()));
        }
        return watchListItems;
    }

    private void updateWatchlist(List<WatchListItm> watchListItems) {
        if (watchListAdapter == null) {
            watchListAdapter = new WatchListAdapter(watchListItems);
            watchlistRecyclerView.setAdapter(watchListAdapter);
        } else {
            watchListAdapter.updateWatchlist(watchListItems);
        }
    }
}
