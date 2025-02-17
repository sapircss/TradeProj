package com.example.tradeproj.handlers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.example.tradeproj.Models.UserPortfolio;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
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

    // ✅ **Fetch user's portfolio from Firebase**
    public CompletableFuture<UserPortfolio> getUserPortfolio() {
        CompletableFuture<UserPortfolio> future = new CompletableFuture<>();
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            Log.e(TAG, "❌ User not logged in, cannot fetch portfolio.");
            future.completeExceptionally(new Exception("User not logged in"));
            return future;
        }

        String userId = user.getUid();
        Log.d(TAG, "🔍 Fetching portfolio for user: " + userId);

        databaseReference.child(userId).child("portfolio")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            UserPortfolio portfolio = dataSnapshot.getValue(UserPortfolio.class);
                            future.complete(portfolio);
                        } else {
                            Log.d(TAG, "❌ No portfolio found. Creating new one...");
                            UserPortfolio newPortfolio = new UserPortfolio(userId, 10000.0);
                            updateUserPortfolio(newPortfolio);
                            future.complete(newPortfolio);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "❌ Error loading portfolio", databaseError.toException());
                        future.completeExceptionally(databaseError.toException());
                    }
                });

        return future;
    }

    // ✅ **Update user's portfolio in Firebase**
    public void updateUserPortfolio(UserPortfolio portfolio) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ Cannot update portfolio, user not logged in!");
            return;
        }

        String userId = user.getUid();
        databaseReference.child(userId).child("portfolio").setValue(portfolio)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Portfolio successfully updated"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Portfolio update failed", e));
    }

    // ✅ **Deposit cash into user's account**
    public void depositCash(double amount, Runnable onSuccess, Runnable onFailure) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ Cannot deposit cash, user not logged in!");
            onFailure.run();
            return;
        }

        getUserPortfolio().thenAccept(portfolio -> {
            portfolio.updateCashBalance(amount);
            updateUserPortfolio(portfolio);

            Log.d(TAG, "✅ Deposited $" + amount + " into account");
            onSuccess.run();
        }).exceptionally(e -> {
            Log.e(TAG, "❌ Error depositing cash", e);
            onFailure.run();
            return null;
        });
    }

    // ✅ **Buy stock and update portfolio**
    public void buyStock(String symbol, int quantity, double price, Runnable onSuccess, Runnable onFailure) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ Cannot buy stock, user not logged in!");
            return;
        }

        getUserPortfolio().thenAccept(portfolio -> {
            double totalCost = quantity * price;

            if (portfolio.getCashBalance() < totalCost) {
                Log.e(TAG, "❌ Not enough cash to buy stock!");
                onFailure.run();
                return;
            }

            portfolio.updateCashBalance(-totalCost);
            portfolio.addHolding(symbol, quantity, price);
            updateUserPortfolio(portfolio);

            Log.d(TAG, "✅ Purchased " + quantity + " of " + symbol + " @ $" + price);
            onSuccess.run();
        }).exceptionally(e -> {
            Log.e(TAG, "❌ Error fetching portfolio for buyStock", e);
            onFailure.run();
            return null;
        });
    }

    // ✅ **Sell stock and update portfolio**
    public void sellStock(String symbol, int quantity, double price, Runnable onSuccess, Runnable onFailure) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ Cannot sell stock, user not logged in!");
            return;
        }

        getUserPortfolio().thenAccept(portfolio -> {
            if (!portfolio.getHoldings().containsKey(symbol)) {
                Log.e(TAG, "❌ Stock not found in portfolio!");
                onFailure.run();
                return;
            }

            int ownedShares = portfolio.getHoldings().get(symbol).getQuantity();
            if (quantity > ownedShares) {
                Log.e(TAG, "❌ Not enough shares to sell!");
                onFailure.run();
                return;
            }

            double totalSaleValue = quantity * price;

            portfolio.removeHolding(symbol, quantity);
            portfolio.updateCashBalance(totalSaleValue);
            updateUserPortfolio(portfolio);

            Log.d(TAG, "✅ Sold " + quantity + " of " + symbol + " @ $" + price);
            onSuccess.run();
        }).exceptionally(e -> {
            Log.e(TAG, "❌ Error fetching portfolio for sellStock", e);
            onFailure.run();
            return null;
        });
    }

    // ✅ **Fetch favorite stocks from Firebase**
    public CompletableFuture<List<String>> getUserFavorites() {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            Log.e(TAG, "❌ User not logged in, cannot fetch favorites.");
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
                        Log.e(TAG, "❌ Error fetching favorite stocks", error.toException());
                        future.completeExceptionally(error.toException());
                    }
                });

        return future;
    }

    // ✅ **Add stock to user's favorites**
    public void addStockToFavorites(String symbol) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ User not logged in, cannot add to favorites.");
            return;
        }

        String userId = user.getUid();
        databaseReference.child(userId).child("favorites").child(symbol).setValue(symbol)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Added to favorites: " + symbol))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to add to favorites", e));
    }

    // ✅ **Remove stock from user's favorites**
    public void removeStockFromFavorites(String symbol) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ User not logged in, cannot remove from favorites.");
            return;
        }

        String userId = user.getUid();
        databaseReference.child(userId).child("favorites").child(symbol).removeValue()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Removed from favorites: " + symbol))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to remove from favorites", e));
    }
}
