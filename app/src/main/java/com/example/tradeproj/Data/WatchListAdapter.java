package com.example.tradeproj.Data;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tradeproj.items.WatchListItm; // ✅ Fixed Import
import com.example.tradeproj.R;
import java.util.ArrayList;
import java.util.List;

public class WatchListAdapter extends RecyclerView.Adapter<WatchListAdapter.ViewHolder> {
    private List<WatchListItm> watchlist = new ArrayList<>();

    public WatchListAdapter(List<WatchListItm> watchlist) {
        if (watchlist != null) {
            this.watchlist = watchlist;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_watchlist, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WatchListItm item = watchlist.get(position);
        holder.symbol.setText(item.getSymbol());
        holder.price.setText(String.format("$%.2f", item.getPrice()));
        holder.change.setText(String.format("%.2f%%", item.getPrice_change()));

        // Set color based on stock price change
        int color = item.getPrice_change() >= 0 ?
                holder.itemView.getContext().getColor(R.color.positive) :
                holder.itemView.getContext().getColor(R.color.negative);
        holder.change.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return watchlist != null ? watchlist.size() : 0;
    }

    /**
     * Update the watchlist with new items.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void updateWatchlist(List<WatchListItm> newWatchlist) {
        if (newWatchlist != null) {
            this.watchlist.clear();
            this.watchlist.addAll(newWatchlist);
            notifyDataSetChanged();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView symbol, price, change;

        ViewHolder(View itemView) {
            super(itemView);
            symbol = itemView.findViewById(R.id.watchlist_item_symbol);
            price = itemView.findViewById(R.id.watchlist_item_price);
            change = itemView.findViewById(R.id.watchlist_item_change);
        }
    }
}
