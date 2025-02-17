package com.example.tradeproj.items;

import java.util.Date;

public class StockItem {
    private String symbol;
    private double price;
    private double change;
    private double percentChange;
    private long volume;
    private Date lastUpdated;
    private boolean isFavorite;
    private double previousPrice;  // ✅ Track previous price for change calculation

    public StockItem(String symbol, double price, double change, double percentChange, long volume) {
        this.symbol = symbol;
        this.price = price;
        this.change = change;
        this.percentChange = percentChange;
        this.volume = volume;
        this.lastUpdated = new Date();
        this.isFavorite = false;
        this.previousPrice = price; // ✅ Initialize previous price
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public double getChange() {
        return change;
    }

    public double getPercentChange() {
        return percentChange;
    }

    public long getVolume() {
        return volume;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public double getPreviousPrice() {
        return previousPrice;
    }

    // ✅ **NEW: Get percentage price change since last update**
    public double getPriceChangePercentage() {
        if (previousPrice == 0) return 0;
        return ((price - previousPrice) / previousPrice) * 100;
    }

    // 🔹 **Update stock price dynamically**
    public void updateStock(double newPrice, double change, double percentChange, long volume) {
        this.previousPrice = this.price;  // ✅ Store old price before updating
        this.price = newPrice;
        this.change = change;
        this.percentChange = percentChange;
        this.volume = volume;
        this.lastUpdated = new Date();
    }

    @Override
    public String toString() {
        return "StockItem{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", change=" + change +
                ", percentChange=" + percentChange +
                ", volume=" + volume +
                ", lastUpdated=" + lastUpdated +
                ", previousPrice=" + previousPrice +
                '}';
    }
}
