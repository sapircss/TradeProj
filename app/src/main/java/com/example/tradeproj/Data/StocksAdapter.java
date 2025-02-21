package com.example.tradeproj.Data;

import android.annotation.SuppressLint;
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
import com.example.tradeproj.items.StockItem;
import java.util.List;

public class StocksAdapter extends RecyclerView.Adapter<StocksAdapter.StockViewHolder> {
    private List<StockItem> stockList;
    private final Context context;
    private final OnStockClickListener stockClickListener;
    private final FirebaseManager firebaseManager;

    public interface OnStockClickListener {
        void onStockClick(StockItem stock);
    }

    public StocksAdapter(List<StockItem> stockList, Context context, OnStockClickListener listener) {
        this.stockList = stockList;
        this.context = context;
        this.stockClickListener = listener;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        StockItem stock = stockList.get(position);
        holder.symbol.setText(stock.getSymbol());
        holder.price.setText(String.format("$%.2f", stock.getPrice()));
        holder.change.setText(String.format("%.2f%%", stock.getPercentChange()));

        // ✅ Set color based on stock price change (green if positive, red if negative)
        int color = stock.getPercentChange() >= 0 ?
                context.getColor(R.color.positive) :
                context.getColor(R.color.negative);
        holder.change.setTextColor(color);

        // ✅ Initialize favorite button state from database
        firebaseManager.getUserFavorites().thenAccept(favorites -> {
            boolean isFavorite = favorites.contains(stock.getSymbol());
            stock.setFavorite(isFavorite);
            holder.favoriteButton.setImageResource(isFavorite ?
                    R.drawable.ic_star_filled : R.drawable.ic_star_border);
        });

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
        });

        // ✅ Handle Stock Item Click
        holder.itemView.setOnClickListener(v -> stockClickListener.onStockClick(stock));
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateStocks(List<StockItem> newStockList) {
        this.stockList = newStockList;
        notifyDataSetChanged();
    }

    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView symbol, price, change;
        ImageButton favoriteButton;

        public StockViewHolder(View itemView) {
            super(itemView);
            symbol = itemView.findViewById(R.id.stock_symbol);
            price = itemView.findViewById(R.id.stock_price);
            change = itemView.findViewById(R.id.stock_change);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
        }
    }
}