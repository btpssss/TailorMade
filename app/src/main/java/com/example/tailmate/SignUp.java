package com.example.tailmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SignUp extends AppCompatActivity {

    EditText name, email, phone;
    Button getOtp;
    CountryCodePicker cpp;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        name = (EditText) findViewById(R.id.Name);
        email= (EditText) findViewById(R.id.email);
        phone = (EditText) findViewById(R.id.mobile);
        getOtp = (Button) findViewById(R.id.get_otp);
        cpp = (CountryCodePicker) findViewById(R.id.cpp);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        getOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phoneNumber = phone.getText().toString();
                String Name = name.getText().toString();
                String eMail = email.getText().toString();
                if(Name.isEmpty() || phoneNumber.isEmpty() || eMail.isEmpty())
                {
                    Toast.makeText(SignUp.this, "Enter all the details.", Toast.LENGTH_SHORT).show();
                }
                else{


                    cpp.registerCarrierNumberEditText(phone);
                    if(cpp.isValidFullNumber())
                    {
                        String number = cpp.getFormattedFullNumber();
                        String hash = generateUniqueID(cpp.getFullNumberWithPlus());
                        System.out.println("+++++++++++++++++++"+hash+"++++++++++++++++++++++++");
                        DocumentReference documentRef = firebaseFirestore.collection("Shop").document(hash);
                        Task<DocumentSnapshot> future = documentRef.get();
                        future.addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                System.out.println(documentSnapshot.exists());
                                if(documentSnapshot.exists())
                                {
                                    Toast.makeText(SignUp.this, "User with the given phone number already exists", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                showLoadingDialog();
                                PhoneAuthProvider.getInstance().verifyPhoneNumber(number, 60, TimeUnit.SECONDS, SignUp.this,
                                        new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                            @Override
                                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                                            }

                                            @Override
                                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                                Toast.makeText(SignUp.this, "Verification Failed", Toast.LENGTH_SHORT).show();
                                                dismissLoadingDialog();
                                            }

                                            @Override
                                            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                                super.onCodeSent(s, forceResendingToken);
                                                Toast.makeText(SignUp.this, "Verification Code Sent", Toast.LENGTH_SHORT).show();
                                                Intent in = new Intent(SignUp.this, OTP.class);
                                                in.putExtra("Name", Name);
                                                in.putExtra("Email", eMail);
                                                in.putExtra("Number", number);
                                                in.putExtra("Uid", hash);
                                                in.putExtra("Src", "SignUp");
                                                in.putExtra("VId", s);
                                                startActivity(in);
                                                dismissLoadingDialog();
                                            }
                                        });

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                               // Toast.makeText(SignUp.this, "User doesnt exists", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        });

                    }
                    else
                    {
                        Toast.makeText(SignUp.this, "Invalid Mobile Number.", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(SignUp.this, MainActivity.class));
    }


    private ProgressDialog progressDialog;

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void dismissLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private static String generateUniqueID(String phoneNumber) {
        String hash = phoneNumber;

        try {
            // Create an instance of the MD5 hashing algorithm
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Convert the phone number to bytes and generate the hash
            md.update(phoneNumber.getBytes());
            byte[] digest = md.digest();

            // Convert the byte array to a BigInteger
            BigInteger bigInt = new BigInteger(1, digest);

            // Convert the BigInteger to a hexadecimal string
            hash = bigInt.toString(16);
        } catch (NoSuchAlgorithmException e) {
            // Handle exceptions related to the hashing algorithm
            e.printStackTrace();
        }

        return hash;
    }


}