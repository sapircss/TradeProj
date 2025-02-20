package com.example.tradeproj.Data;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tradeproj.R;
import com.example.tradeproj.handlers.FirebaseManager;
import com.example.tradeproj.items.WatchListItm;
import java.util.List;

public class WatchListAdapter extends RecyclerView.Adapter<WatchListAdapter.WatchListViewHolder> {
    private List<WatchListItm> watchList;
    private final Context context;
    private final FirebaseManager firebaseManager;

    public WatchListAdapter(List<WatchListItm> watchList, Context context) {
        this.watchList = watchList;
        this.context = context;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    @NonNull
    @Override
    public WatchListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_watchlist, parent, false);
        return new WatchListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WatchListViewHolder holder, int position) {
        WatchListItm stock = watchList.get(position);
        holder.symbol.setText(stock.getSymbol());
        holder.price.setText(String.format("$%.2f", stock.getPrice()));
        holder.change.setText(String.format("%.2f%%", stock.getChangePercentage()));

        int color = stock.getChangePercentage() >= 0 ?
                context.getColor(R.color.positive) :
                context.getColor(R.color.negative);
        holder.change.setTextColor(color);

        // ✅ Set correct favorite status without extra Firebase calls
        holder.favoriteButton.setImageResource(stock.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_border);

        // ✅ Handle Favorite Button Click
        holder.favoriteButton.setOnClickListener(v -> {
            boolean newFavoriteStatus = !stock.isFavorite();
            stock.setFavorite(newFavoriteStatus);
            holder.favoriteButton.setImageResource(newFavoriteStatus ?
                    R.drawable.ic_star_filled : R.drawable.ic_star_border);

            if (newFavoriteStatus) {
                firebaseManager.addStockToFavorites(stock.getSymbol());
                Toast.makeText(context, stock.getSymbol() + " added to favorites", Toast.LENGTH_SHORT).show();
            } else {
                firebaseManager.removeStockFromFavorites(stock.getSymbol());
                Toast.makeText(context, stock.getSymbol() + " removed from favorites", Toast.LENGTH_SHORT).show();
            }

            notifyItemChanged(position); // ✅ Only update clicked item, prevent flickering
        });
    }

    @Override
    public int getItemCount() {
        return watchList.size();
    }

    public void updateWatchlist(List<WatchListItm> newWatchList) {
        this.watchList = newWatchList;
        notifyDataSetChanged();
    }

    public static class WatchListViewHolder extends RecyclerView.ViewHolder {
        TextView symbol, price, change;
        ImageButton favoriteButton;

        public WatchListViewHolder(View itemView) {
            super(itemView);
            symbol = itemView.findViewById(R.id.watchlist_item_symbol);
            price = itemView.findViewById(R.id.watchlist_item_price);
            change = itemView.findViewById(R.id.watchlist_item_change);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
        }
    }
}
