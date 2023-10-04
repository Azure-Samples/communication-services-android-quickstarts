package com.example.videocallingquickstart;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.azure.android.communication.common.MicrosoftTeamsUserIdentifier;

import androidx.appcompat.app.AppCompatActivity;

import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;

public class TokenActivity extends Activity {

    private EditText tokenEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);

        tokenEditText = findViewById(R.id.token);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String savedToken = sharedPreferences.getString("TOKEN", "");

        tokenEditText.setText(savedToken);

    }

    public void continueButtonClicked(View v) {

        String token =  tokenEditText.getText().toString();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("TOKEN", token);
        editor.apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("TOKEN", token);
        startActivity(intent);
    }
}