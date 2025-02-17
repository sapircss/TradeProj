package com.example.tradeproj.items;

public class WatchListItm {

    private String symbol;
    private double price;
    private double price_change;

    public WatchListItm(String symbol, double price, double price_change) {
        this.symbol = symbol;
        this.price = price;
        this.price_change = price_change;
    }

    public double getPrice_change() { return price_change; }

    public void setPrice_change(double price_change) { this.price_change = price_change; }

    public double getPrice() { return price; }

    public void setPrice(double price) { this.price = price; }

    public String getSymbol() { return symbol; }

    public void setSymbol(String symbol) { this.symbol = symbol; }

    @Override
    public String toString() {
        return "WatchListItm{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", price_change=" + price_change +
                '}';
    }
}
