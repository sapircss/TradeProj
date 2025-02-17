package com.example.tradeproj.Fragments;

import android.os.Bundle;
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
import com.example.tradeproj.Models.User;
import com.example.tradeproj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Register extends Fragment {
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private EditText emailField, passwordField, confirmPasswordField, phoneField;
    private static final String TAG = "RegisterFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        emailField = view.findViewById(R.id.etEmail);
        passwordField = view.findViewById(R.id.etPassword);
        confirmPasswordField = view.findViewById(R.id.repeatPassword);
        phoneField = view.findViewById(R.id.etPhoneNumber);
        Button registerButton = view.findViewById(R.id.btnSignIn);

        registerButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            String confirmPassword = confirmPasswordField.getText().toString().trim();
            String phone = phoneField.getText().toString().trim();

            if (validateInputs(email, password, confirmPassword, phone)) {
                registerUser(email, password, phone, view);
            }
        });

        return view;
    }

    private boolean validateInputs(String email, String password, String confirmPassword, String phone) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword) || TextUtils.isEmpty(phone)) {
            Toast.makeText(getActivity(), "All fields are required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getActivity(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void registerUser(String email, String password, String phone, View view) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid(), email, phone, view);
                        }
                    } else {
                        Log.e(TAG, "User registration failed", task.getException());
                        Toast.makeText(getActivity(), "Registration Failed!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(String userId, String email, String phone, View view) {
        User newUser = new User(email, phone);

        usersRef.child(userId).setValue(newUser).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getActivity(), "User registered successfully!", Toast.LENGTH_LONG).show();
                Navigation.findNavController(view).navigate(R.id.action_register_to_login);
            } else {
                Toast.makeText(getActivity(), "Database error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
