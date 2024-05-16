package com.example.tablet;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText nameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // Name input
        nameInput = findViewById(R.id.name_input);

        //Start button
        Button startButton = findViewById(R.id.start_button);

        startButton.setOnClickListener(view -> {
            String userName = nameInput.getText().toString().trim();
            if (userName.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter your name before starting.", Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(MainActivity.this, DataCollectionActivity.class);
                intent.putExtra("USER_NAME", userName); // Pass the user name to DataCollectionActivity
                startActivity(intent);
            }
        });
    }

}