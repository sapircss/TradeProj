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
    private double previousPrice;
    private int quantity; // ✅ NEW: Store quantity
    private double buyPrice; // ✅ NEW: Store buy price

    public StockItem(String symbol, double price, double change, double percentChange, long volume, int quantity, double buyPrice) {
        this.symbol = symbol;
        this.price = price;
        this.change = change;
        this.percentChange = percentChange;
        this.volume = volume;
        this.lastUpdated = new Date();
        this.isFavorite = false;
        this.quantity = quantity; // ✅ Initialize quantity
        this.buyPrice = buyPrice; // ✅ Initialize buy price
    }

    // ✅ NEW: Get P&L (Profit/Loss)
    public double getPnl() {
        return (price - buyPrice) * quantity;
    }

    // ✅ NEW: Get P&L Percentage
    public double getPnlPercentage() {
        if (buyPrice == 0) return 0;
        return ((price - buyPrice) / buyPrice) * 100;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
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

    public double getPriceChangePercentage() {
        if (previousPrice == 0) return 0;
        return ((price - previousPrice) / previousPrice) * 100;
    }

    public void updateStock(double newPrice, double change, double percentChange, long volume) {
        this.previousPrice = this.price;
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
                ", quantity=" + quantity +
                ", buyPrice=" + buyPrice +
                ", PnL=" + getPnl() +
                ", PnL%=" + getPnlPercentage() +
                '}';
    }
}
