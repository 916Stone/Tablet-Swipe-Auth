package com.example.tablet;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class DataCollectionActivity extends AppCompatActivity {
    private TextView swipeCounterView;
    private List<String> swipeData;
    private int swipeCounter = 0;
    private float lastX, lastY;
    private long lastTime;
    private float lastVelocityX, lastVelocityY;
    private long initialTime;
    private int orientation;
    private int movementId;

    private static final String TAG = "DataCollectionActivity";
    private int lastLoggedIndex = 0; // Initial value set to 0, assuming no data has been logged yet

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_data_collection);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        swipeData = new ArrayList<>();
        swipeData.add("Index,MovementID,PointerID,EventTime,X,Y," +
                "Pressure,Size,VelocityX,VelocityY,AccelerationX,AccelerationY," +
                "Angle,Duration,Distance,Orientation"); // Header for CSV file

        swipeCounterView = findViewById(R.id.swipeCounterView); // Link the Java variable to the XML UI element
        swipeCounterView.setText(String.valueOf(swipeCounter)); // Initialize text with current swipe counter

        String userName = getIntent().getStringExtra("USER_NAME"); // Retrieve the user name from MainActivity
        TextView userNameView = findViewById(R.id.userNameView); // Link the Java variable to the XML UI element
        userNameView.setText(userName); // Set the text to the user name

        orientation = getResources().getConfiguration().orientation;

        Button doneButton = findViewById(R.id.doneButton);
        doneButton.setOnClickListener(view -> {
            // Create and show an AlertDialog for confirmation
            new AlertDialog.Builder(this)
                    .setTitle("Confirm")
                    .setMessage("You have recorded " + swipeCounter + " swipes. Do you want to proceed?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // User confirms to proceed
                        Intent intent = new Intent(DataCollectionActivity.this, TrainingActivity.class);
                        intent.putStringArrayListExtra("SWIPE_DATA", (ArrayList<String>) swipeData);
                        startActivity(intent);
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        // User cancels the operation
                        dialog.dismiss();
                    })
                    .create()
                    .show();
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        long time = event.getEventTime();
        float pressure = event.getPressure(pointerIndex);
        float size = event.getSize(pointerIndex);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Initialize swipe data collection
                lastX = x;
                lastY = y;
                lastTime = time;
                initialTime = time;
                lastVelocityX = 0;
                lastVelocityY = 0;
                movementId = 0;
                swipeData.add(formatData(swipeCounter, movementId, pointerId, time, x, y, pressure, size,
                        0, 0, 0, 0, 0, 0, 0, orientation));
                movementId++;
                break;

            case MotionEvent.ACTION_MOVE:
                // Collect swipe data
                float velocityX = (x - lastX) / (time - lastTime);
                float velocityY = (y - lastY) / (time - lastTime);
                float accelerationX = (velocityX - lastVelocityX) / (time - lastTime);
                float accelerationY = (velocityY - lastVelocityY) / (time - lastTime);
                float angle = (float) Math.toDegrees(Math.atan2(y - lastY, x - lastX));
                float distance = (float) Math.sqrt(Math.pow(x - lastX, 2) + Math.pow(y - lastY, 2));
                long duration = time - initialTime;
                swipeData.add(formatData(swipeCounter, movementId, pointerId, time, x, y, pressure, size,
                        velocityX, velocityY, accelerationX, accelerationY, angle, duration, distance, orientation));
                movementId++;

                lastX = x;
                lastY = y;
                lastTime = time;
                lastVelocityX = velocityX;
                lastVelocityY = velocityY;
                break;

            case MotionEvent.ACTION_UP:
                // Finalize swipe data and send to Firebase
                // Print the data for this swipe
                swipeCounter++;
                swipeCounterView.setText(String.valueOf(swipeCounter));
                while (lastLoggedIndex < swipeData.size()) {
                    Log.d(TAG, swipeData.get(lastLoggedIndex));
                    lastLoggedIndex++;
                }

                break;
        }
        return true;
    }

    private String formatData(int index, int movementID, int pointerId, long time, float x, float y, float pressure, float size,
                              float velocityX, float velocityY, float accelerationX, float accelerationY,
                              float angle, long duration, float distance, int orientation) {
        return String.format(Locale.US, "%d,%d,%d,%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%d,%f,%d",
                index, movementID, pointerId, time, x, y, pressure, size, velocityX, velocityY, accelerationX, accelerationY,
                angle, duration, distance, orientation);
    }

}
