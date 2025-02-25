package com.example.tradeproj.handlers;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.example.tradeproj.Models.UserPortfolio;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class FirebaseManager {
    private static FirebaseManager instance;
    private final DatabaseReference databaseReference;
    private final FirebaseAuth firebaseAuth;
    private static final String TAG = "FirebaseManager";

    private FirebaseManager() {
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    //  Fetch user's portfolio from Firebase (ASYNC)
    public CompletableFuture<UserPortfolio> getUserPortfolio() {
        CompletableFuture<UserPortfolio> future = new CompletableFuture<>();
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            Log.e(TAG, "User not logged in, cannot fetch portfolio.");
            future.completeExceptionally(new Exception("User not logged in"));
            return future;
        }

        String userId = user.getUid();
        Log.d(TAG, " Fetching portfolio for user: " + userId);

        databaseReference.child(userId).child("portfolio")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            UserPortfolio portfolio = dataSnapshot.getValue(UserPortfolio.class);

                            //  Ensure portfolio has valid structure
                            if (portfolio == null) {
                                portfolio = new UserPortfolio(userId, 10000.0);
                            }
                            if (portfolio.getHoldings() == null) {
                                portfolio.setHoldings(new HashMap<>());
                            }

                            future.complete(portfolio);
                        } else {
                            Log.d(TAG, " No portfolio found. Creating new one...");
                            UserPortfolio newPortfolio = new UserPortfolio(userId, 10000.0);
                            updateUserPortfolio(newPortfolio, false);
                            future.complete(newPortfolio);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, " Error loading portfolio", databaseError.toException());
                        future.completeExceptionally(databaseError.toException());
                    }
                });

        return future;
    }

    //  Update user's portfolio in Firebase
    public void updateUserPortfolio(UserPortfolio portfolio, boolean storePrices) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, " Cannot update portfolio, user not logged in!");
            return;
        }

        String userId = user.getUid();
        databaseReference.child(userId).child("portfolio").setValue(portfolio)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, " Portfolio successfully updated");

                    //  Convert holdings to last known prices and store them
                    if (storePrices) {
                        Map<String, Double> lastPrices = new HashMap<>();
                        for (Map.Entry<String, UserPortfolio.Holding> entry : portfolio.getHoldings().entrySet()) {
                            lastPrices.put(entry.getKey(), entry.getValue().getCurrentPrice()); //  Extract Double price
                        }
                        storeLastStockPrices(lastPrices); //  Pass converted Map
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, " Portfolio update failed", e));
    }


    // Store last known stock prices in Firebase
    public void storeLastStockPrices(Map<String, Double> lastPrices) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot store stock prices, user not logged in!");
            return;
        }

        String userId = user.getUid();
        DatabaseReference stockPricesRef = databaseReference.child(userId).child("lastStockPrices");

        if (lastPrices == null || lastPrices.isEmpty()) {
            Log.d(TAG, "No last prices to store.");
            return;
        }

        stockPricesRef.updateChildren(new HashMap<>(lastPrices))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Stored last known stock prices in Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to store last known stock prices", e));
    }


    public CompletableFuture<Map<String, Double>> getLastStockPrices() {
        CompletableFuture<Map<String, Double>> future = new CompletableFuture<>();
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            Log.e(TAG, " User not logged in, cannot fetch last stock prices.");
            future.completeExceptionally(new Exception("User not logged in"));
            return future;
        }

        String userId = user.getUid();
        databaseReference.child(userId).child("lastStockPrices")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Map<String, Double> lastPrices = new HashMap<>();
                        for (DataSnapshot stockSnapshot : snapshot.getChildren()) {
                            String symbol = stockSnapshot.getKey();
                            Double lastPrice = stockSnapshot.getValue(Double.class);
                            if (symbol != null && lastPrice != null) {
                                lastPrices.put(symbol, lastPrice);
                            }
                        }
                        future.complete(lastPrices);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, " Error fetching last stock prices", error.toException());
                        future.completeExceptionally(error.toException());
                    }
                });

        return future;
    }
    //  Deposit cash into user's account
    public void depositCash(double amount, Runnable onSuccess, Runnable onFailure) {
        getUserPortfolio().thenAccept(portfolio -> {
            if (portfolio == null) {
                Log.e(TAG, " Portfolio not found, cannot deposit cash.");
                onFailure.run();
                return;
            }

            portfolio.updateCashBalance(amount);
            updateUserPortfolio(portfolio, false);

            Log.d(TAG, " Deposited $" + amount + " into account");
            onSuccess.run();
        }).exceptionally(e -> {
            Log.e(TAG, " Error depositing cash", e);
            onFailure.run();
            return null;
        });
    }

    //  Buy stock and update portfolio
    public void buyStock(String symbol, int quantity, double price, Runnable onSuccess, Runnable onFailure) {
        getUserPortfolio().thenAccept(portfolio -> {
            if (portfolio == null) {
                onFailure.run();
                return;
            }

            double totalCost = quantity * price;
            if (portfolio.getCashBalance() < totalCost) {
                onFailure.run();
                return;
            }

            portfolio.updateCashBalance(-totalCost);
            portfolio.addHolding(symbol, quantity, price);
            updateUserPortfolio(portfolio, true);

            onSuccess.run();
        }).exceptionally(e -> {
            onFailure.run();
            return null;
        });
    }

    //  Sell stock and update portfolio
    public void sellStock(String symbol, int quantity, double price, Runnable onSuccess, Runnable onFailure) {
        getUserPortfolio().thenAccept(portfolio -> {
            if (portfolio == null) {
                onFailure.run();
                return;
            }

            UserPortfolio.Holding holding = portfolio.getHoldings().getOrDefault(symbol, new UserPortfolio.Holding(0, 0, 0));
            int ownedShares = holding.getQuantity();

            if (ownedShares == 0 || quantity > ownedShares) {
                onFailure.run();
                return;
            }

            double totalSaleValue = quantity * price;
            portfolio.removeHolding(symbol, quantity);
            portfolio.updateCashBalance(totalSaleValue);

            updateUserPortfolio(portfolio, true);
            onSuccess.run();
        }).exceptionally(e -> {
            onFailure.run();
            return null;
        });
    }

    // Add stock to favorites
    public void addStockToFavorites(String symbol) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, " User not logged in, cannot add to favorites.");
            return;
        }

        String userId = user.getUid();
        databaseReference.child(userId).child("favorites").child(symbol).setValue(symbol)
                .addOnSuccessListener(aVoid -> Log.d(TAG, " Added to favorites: " + symbol))
                .addOnFailureListener(e -> Log.e(TAG, " Failed to add to favorites", e));
    }

    //  Remove stock from favorites
    public void removeStockFromFavorites(String symbol) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, " User not logged in, cannot remove from favorites.");
            return;
        }

        String userId = user.getUid();
        databaseReference.child(userId).child("favorites").child(symbol).removeValue()
                .addOnSuccessListener(aVoid -> Log.d(TAG, " Removed from favorites: " + symbol))
                .addOnFailureListener(e -> Log.e(TAG, " Failed to remove from favorites", e));
    }

    public CompletableFuture<List<String>> getUserFavorites() {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            Log.e(TAG, " User not logged in, cannot fetch favorites.");
            future.completeExceptionally(new Exception("User not logged in"));
            return future;
        }

        String userId = user.getUid();
        databaseReference.child(userId).child("favorites")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<String> favorites = new ArrayList<>();
                        for (DataSnapshot stockSnapshot : snapshot.getChildren()) {
                            String symbol = stockSnapshot.getValue(String.class);
                            if (symbol != null) favorites.add(symbol);
                        }
                        future.complete(favorites);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, " Error fetching favorite stocks", error.toException());
                        future.completeExceptionally(error.toException());
                    }
                });

        return future;
    }

}
