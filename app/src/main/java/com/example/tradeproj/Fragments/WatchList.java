package com.example.tradeproj.Fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_watch_list, container, false);

        watchlistRecyclerView = view.findViewById(R.id.watchlist_recycler_view);
        watchlistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        firebaseManager = FirebaseManager.getInstance();
        finnhubApi = FinnhubApi.getInstance(getContext());

        // Fetch the favorite stocks from Firebase
        fetchFavoriteStocks();

        return view;
    }

    private void fetchFavoriteStocks() {
        firebaseManager.getUserFavorites().thenAccept(favoriteSymbols -> {
            if (favoriteSymbols == null || favoriteSymbols.isEmpty()) {
                return; // No favorite stocks
            }

            // Fetch the stock prices for the favorite symbols
            fetchStockPrices(favoriteSymbols);
        });
    }

    private void fetchStockPrices(List<String> favoriteSymbols) {
        finnhubApi.fetchStockPrices(favoriteSymbols, stockItems -> {
            if (stockItems != null && !stockItems.isEmpty()) {
                // Convert stockItems into WatchListItm objects and update the adapter
                List<WatchListItm> watchListItems = convertToWatchListItems(stockItems);
                updateWatchlist(watchListItems);
            }
        });
    }

    private List<WatchListItm> convertToWatchListItems(List<StockItem> stockItems) {
        // Convert StockItem objects to WatchListItm objects
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
