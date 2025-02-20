package com.example.tradeproj.Activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.tradeproj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "🎬 MainActivity started!");

        // ✅ Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Removed signOut() to keep the user logged in (if needed)
        // mAuth.signOut();

        // ✅ Initialize Firebase Realtime Database with correct URL
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://tradeapp-b6a77-default-rtdb.firebaseio.com/");
        database.setPersistenceEnabled(true);  // ✅ Enables offline support

        Log.d(TAG, "✅ Firebase Database Initialized");

        // ✅ Initialize Navigation Controller
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            Log.e(TAG, "❌ NavHostFragment not found! Check activity_main.xml");
            Toast.makeText(this, "Navigation error!", Toast.LENGTH_SHORT).show();
            return;
        }

        navController = navHostFragment.getNavController();

        // ✅ Ensure the app always starts on the Login screen
        Log.d(TAG, "🔄 Navigating to Login screen...");
        navController.navigate(R.id.login);
    }


    // ✅ Method to be called after successful login
    public void navigateToTrade() {
        if (navController != null) {
            Log.d(TAG, "🚀 Navigating to TradeFragment from MainActivity!");
            navController.navigate(R.id.trade);
        } else {
            Log.e(TAG, "❌ NavController is null! Cannot navigate.");
            Toast.makeText(this, "❌ Navigation failed!", Toast.LENGTH_SHORT).show();
        }
    }
}