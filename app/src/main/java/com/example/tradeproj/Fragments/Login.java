package com.example.tradeproj.Fragments;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.tradeproj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends Fragment {
    private FirebaseAuth mAuth;
    private EditText emailField, passwordField;
    private static final String TAG = "LoginFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        emailField = view.findViewById(R.id.LoginEmail);
        passwordField = view.findViewById(R.id.LoginPassword);
        Button loginButton = view.findViewById(R.id.btnToTrade);
        Button regButton = view.findViewById(R.id.btnToRegister);

        //  Ensure Email Field Accepts Text and Suggests Emails
        emailField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setHint("Enter Email");
        emailField.setFocusable(true);
        emailField.setFocusableInTouchMode(true);

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(getActivity(), "Email and Password are required", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        regButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_login_to_register));

        return view;
    }


    private void loginUser(String email, String password) {
        Log.d(TAG, " Attempting login with Email: " + email);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, " User logged in: " + user.getEmail());
                            Toast.makeText(requireActivity(), " Login Successful!", Toast.LENGTH_SHORT).show();
                            checkUserInDatabase(user.getEmail(), requireView()); //  Search by email now
                        } else {
                            Log.e(TAG, " Login succeeded, but user object is null!");
                            Toast.makeText(requireActivity(), " Error: User object is null!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, " Login Failed: " + task.getException());
                        Toast.makeText(requireActivity(), " Login Failed! Check credentials.", Toast.LENGTH_LONG).show();
                    }
                });
    }


    private void checkUserInDatabase(String email, View view) {
        Log.d(TAG, "Searching user in database by Email: " + email);

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users"); // Make sure it's lowercase "users"

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Database Snapshot: " + snapshot.toString());

                if (snapshot.exists()) {
                    Log.d(TAG, "User found in database.");
                    Toast.makeText(requireActivity(), "User Verified! Redirecting...", Toast.LENGTH_SHORT).show();

                    if (isAdded() && view != null) {
                        Log.d(TAG, " Navigating to Trade...");
                        Navigation.findNavController(view).navigate(R.id.action_login_to_trade);
                    } else {
                        Log.e(TAG, " Fragment not attached or view is null. Cannot navigate.");
                        Toast.makeText(requireActivity(), " Navigation failed!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e(TAG, " User not found in database!");
                    Toast.makeText(requireActivity(), " User not found! Register first.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, " Database query failed: " + error.getMessage());
                Toast.makeText(requireActivity(), "Database error!", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
