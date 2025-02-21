package com.example.tradeproj.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WatchList extends Fragment {
    private RecyclerView watchlistRecyclerView;
    private WatchListAdapter watchListAdapter;
    private FirebaseManager firebaseManager;
    private FinnhubApi finnhubApi;
    private Button backToTradeButton, backToPortfolioButton;

    private static final String TAG = "WatchListFragment";

    private Set<String> favoriteStocks = new HashSet<>(); // ✅ Keep track of user's favorite stocks

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watch_list, container, false);

        watchlistRecyclerView = view.findViewById(R.id.watchlist_recycler_view);
        watchlistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        backToTradeButton = view.findViewById(R.id.back_to_trade);
        backToPortfolioButton = view.findViewById(R.id.back_to_portfolio);

        firebaseManager = FirebaseManager.getInstance();
        finnhubApi = FinnhubApi.getInstance(getContext());

        // ✅ Load watchlist initially
        loadWatchlist();

        backToTradeButton.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_watchlist_to_trade));
        backToPortfolioButton.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_watchlist_to_portfolio));

        return view;
    }

    /** ✅ Always reload watchlist when returning */
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "📢 Resuming Watchlist Page, fetching favorite stocks...");
        new Handler(Looper.getMainLooper()).postDelayed(this::loadWatchlist, 500);
    }

    /** ✅ Fetch favorite stocks from Firebase & update UI */
    private void loadWatchlist() {
        firebaseManager.getUserFavorites().thenAccept(favorites -> {
            if (!isAdded()) return; // ✅ Prevent crashes if fragment is no longer attached

            if (favorites == null || favorites.isEmpty()) {
                Log.d(TAG, "⚠ No favorite stocks found.");
                favoriteStocks.clear();
                setWatchlistData(new ArrayList<>()); // ✅ Clear UI
                return;
            }

            // ✅ Store favorite stocks
            favoriteStocks = new HashSet<>(favorites);
            Log.d(TAG, "✅ User Favorites: " + favoriteStocks);

            // ✅ Fetch stock prices for only favorite stocks
            fetchStockPrices(new ArrayList<>(favoriteStocks));
        }).exceptionally(e -> {
            Log.e(TAG, "❌ Error fetching favorite stocks", e);
            return null;
        });
    }

    /** ✅ Fetch stock prices only for favorite stocks */
    private void fetchStockPrices(List<String> favoriteSymbols) {
        if (favoriteSymbols.isEmpty()) {
            Log.d(TAG, "⚠ No favorite stocks to fetch prices for.");
            setWatchlistData(new ArrayList<>()); // Clear UI
            return;
        }

        Log.d(TAG, "🔍 Fetching stock prices for watchlist: " + favoriteSymbols);

        finnhubApi.fetchStockPrices(favoriteSymbols,
                stockItems -> {
                    if (stockItems != null && !stockItems.isEmpty()) {
                        Log.d(TAG, "✅ Successfully fetched stock prices: " + stockItems);
                        requireActivity().runOnUiThread(() -> updateStockListWithFavorites(stockItems)); // ✅ Fixes missing update
                    } else {
                        Log.e(TAG, "⚠ No stock prices returned.");
                        setWatchlistData(new ArrayList<>()); // Clear UI
                    }
                },
                errorMessage -> {
                    Log.e(TAG, "❌ Error fetching stock prices: " + errorMessage);
                    requireActivity().runOnUiThread(() -> setWatchlistData(new ArrayList<>())); // Clear UI on error
                }
        );
    }

    /** ✅ Ensures stocks in the watchlist have correct star status */
    private void updateStockListWithFavorites(List<StockItem> stockItems) {
        for (StockItem stock : stockItems) {
            stock.setFavorite(favoriteStocks.contains(stock.getSymbol())); // ✅ Ensure correct favorite status
        }
        requireActivity().runOnUiThread(() -> displayWatchlist(stockItems)); // ✅ Update UI
    }

    /** ✅ Converts `StockItem` to `WatchListItm` with correct star status */
    private void displayWatchlist(List<StockItem> stockItems) {
        List<WatchListItm> watchListItems = new ArrayList<>();
        for (StockItem stock : stockItems) {
            WatchListItm watchListItem = new WatchListItm(stock.getSymbol(), stock.getPrice(), stock.getPercentChange());
            watchListItem.setFavorite(stock.isFavorite()); // ✅ Use updated favorite status
            watchListItems.add(watchListItem);
        }
        setWatchlistData(watchListItems);
    }

    /** ✅ Updates UI and refreshes watchlist */
    private void setWatchlistData(List<WatchListItm> watchListItems) {
        if (!isAdded()) return; // ✅ Prevent crashes if fragment is detached

        requireActivity().runOnUiThread(() -> {
            if (watchListAdapter == null) {
                watchListAdapter = new WatchListAdapter(watchListItems, requireContext());
                watchlistRecyclerView.setAdapter(watchListAdapter);
            } else {
                watchListAdapter.updateWatchlist(watchListItems);
            }
        });
    }
}
