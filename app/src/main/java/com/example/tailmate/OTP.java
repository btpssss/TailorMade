package com.example.tailmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class OTP extends AppCompatActivity {

    EditText box1, box2, box3, box4, box5, box6;
    TextView tv, tv1;
    Button verify;
    FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        box1 = findViewById(R.id.box1);
        box2 = findViewById(R.id.box2);
        box3 = findViewById(R.id.box3);
        box4 = findViewById(R.id.box4);
        box5 = findViewById(R.id.box5);
        box6 = findViewById(R.id.box6);
        tv = (TextView) findViewById(R.id.textview4);
        tv1 = (TextView) findViewById(R.id.textView5);
        verify = (Button) findViewById(R.id.verify);

        firebaseAuth = FirebaseAuth.getInstance();

        Intent in = getIntent();
        String name = in.getStringExtra("Name");
        String email = in.getStringExtra("Email");
        String number = in.getStringExtra("Number");
        String src = in.getStringExtra("Src");
        String uid = in.getStringExtra("Uid");
        String mVerificationId = in.getStringExtra("VId");
        tv.setText("Enter the 6-digit code sent to\n" + number);
        // Set up listeners
        setListeners();

        tv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i;
                if(src.equals("SignUp"))
                {
                    i = new Intent(OTP.this, SignUp.class);
                }
                else{
                    i = new Intent(OTP.this, LogIn.class);
                }
                startActivity(i);
            }
        });

        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(box1.getText().length()==0 || box2.getText().length()==0 || box3.getText().length()==0 || box4.getText().length()==0 ||
                        box5.getText().length()==0 || box6.getText().length()==0)
                {
                    Toast.makeText(OTP.this, "Enter Correct OTP", Toast.LENGTH_SHORT).show();
                }
                else{
                    showLoadingDialog();
                    String verificationCode = box1.getText().toString()+
                            box2.getText().toString()+
                            box3.getText().toString()+
                            box4.getText().toString()+
                            box5.getText().toString()+
                            box6.getText().toString();

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                    firebaseAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Toast.makeText(OTP.this, "Authentication Successful", Toast.LENGTH_SHORT).show();

                                if(src.equals("SignUp"))
                                {
                                    Intent in = new Intent(OTP.this, ShopDetails.class);
                                    in.putExtra("Name", name);
                                    in.putExtra("Email", email);
                                    in.putExtra("Hash", uid);
                                    startActivity(in);
                                }
                                else
                                {
                                    startActivity(new Intent(OTP.this, HomePage.class));
                                }
                            } else {
                                // Sign in failed, display a message to the user.
                                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                    // The verification code entered was invalid
                                    Toast.makeText(OTP.this, "Invalid Verification Code", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Other errors occurred during sign in
                                    Toast.makeText(OTP.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                            dismissLoadingDialog();
                        }
                    });

                }


            }
        });

    }

    private void setListeners() {
        TextWatcher otpTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.length() == 1) {
                    if (box1.isFocused()) {
                        box2.requestFocus();
                    } else if (box2.isFocused()) {
                        box3.requestFocus();
                    } else if (box3.isFocused()) {
                        box4.requestFocus();
                    } else if (box4.isFocused()) {
                        box5.requestFocus();
                    } else if (box5.isFocused()) {
                        box6.requestFocus();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };

        View.OnKeyListener otpKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent event) {
                if (i == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (box6.isFocused()) {
                        box5.requestFocus();
                        box5.setText("");
                    } else if (box5.isFocused()) {
                        box4.requestFocus();
                        box4.setText("");
                    } else if (box4.isFocused()) {
                        box3.requestFocus();
                        box3.setText("");
                    } else if (box3.isFocused()) {
                        box2.requestFocus();
                        box2.setText("");
                    } else if (box2.isFocused()) {
                        box1.requestFocus();
                        box1.setText("");
                    }
                }
                return false;
            }
        };

        box1.addTextChangedListener(otpTextWatcher);
        box2.addTextChangedListener(otpTextWatcher);
        box3.addTextChangedListener(otpTextWatcher);
        box4.addTextChangedListener(otpTextWatcher);
        box5.addTextChangedListener(otpTextWatcher);
        box6.addTextChangedListener(otpTextWatcher);

        box2.setOnKeyListener(otpKeyListener);
        box3.setOnKeyListener(otpKeyListener);
        box4.setOnKeyListener(otpKeyListener);
        box5.setOnKeyListener(otpKeyListener);
        box6.setOnKeyListener(otpKeyListener);

    }


    @Override
    public void onBackPressed() {}

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
}
