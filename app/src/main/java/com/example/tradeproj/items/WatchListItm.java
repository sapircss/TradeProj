package com.example.tradeproj.items;

public class WatchListItm {
    private String symbol;
    private double price;
    private double changePercentage;
    private boolean isFavorite; // ✅ Add Favorite Property

    public WatchListItm(String symbol, double price, double changePercentage) {
        this.symbol = symbol;
        this.price = price;
        this.changePercentage = changePercentage;
        this.isFavorite = false; // Default to not favorite
    }

    // ✅ Correct Method Names
    public double getChangePercentage() {
        return changePercentage;
    }

    public void setChangePercentage(double changePercentage) {
        this.changePercentage = changePercentage;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    // ✅ Added Favorite Methods
    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }

    @Override
    public String toString() {
        return "WatchListItm{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", changePercentage=" + changePercentage +
                ", isFavorite=" + isFavorite +
                '}';
    }
}
