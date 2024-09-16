package com.example.tablet;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


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
    private float pathLength = 0;
    private float initialVelocityX = 0, initialVelocityY = 0;
    private int directionChanges = 0;
    private float lastAngle = 0;
    private float curvature = 0;

    private static final String TAG = "DataCollectionActivity";
    private int lastLoggedIndex = 0; // Initial value set to 0, assuming no data has been logged yet

    private List<String> rawSwipeData;
    private List<String> statisticalSwipeData;
    private String userName;

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
        rawSwipeData = new ArrayList<>();
        statisticalSwipeData = new ArrayList<>();

        rawSwipeData.add("Index,MovementID,PointerID,EventTime,X,Y,Pressure,Size,VelocityX,VelocityY," +
                "AccelerationX,AccelerationY,Angle,Duration,Distance,Orientation,PathLength," +
                "InitialVelocityX,InitialVelocityY,FinalVelocityX,FinalVelocityY,DirectionChanges,Curvature,JerkMagnitude");

        statisticalSwipeData.add("Index,XMean,XStd,XMin,XMax,YMean,YStd,YMin,YMax,PressureMean,PressureMax,PressureStd," +
                "SizeMean,SizeMax,VelocityXMean,VelocityXMax,VelocityXStd,VelocityYMean,VelocityYMax,VelocityYStd," +
                "AccelerationXMean,AccelerationXMax,AccelerationXStd,AccelerationYMean,AccelerationYMax,AccelerationYStd," +
                "AngleMean,AngleStd,TotalDistance,StraightLineDistance,PathLength,InitialVelocityX,InitialVelocityY," +
                "FinalVelocityX,FinalVelocityY,Duration,DirectionChanges,CurvatureMean,JerkMean");

        swipeCounterView = findViewById(R.id.swipeCounterView); // Link the Java variable to the XML UI element
        swipeCounterView.setText(String.valueOf(swipeCounter)); // Initialize text with current swipe counter

        userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null || userName.isEmpty()) {
            userName = "Unknown User"; // Default value if no name was passed
        }
        TextView userNameView = findViewById(R.id.userNameView); // Link the Java variable to the XML UI element
        userNameView.setText(userName); // Set the text to the user name

        orientation = getResources().getConfiguration().orientation;
        
        Button doneButton = findViewById(R.id.doneButton);
        
        doneButton.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle("Share Data")
                .setMessage("Do you want to share the collected data?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    shareData();
                    promptForNextActivity();
                })
                .setNegativeButton("No", (dialog, which) -> promptForNextActivity())
                .create()
                .show());
    }

    private void promptForNextActivity() {
        new AlertDialog.Builder(this)
                .setTitle("Proceed to Next Activity")
                .setMessage("You have recorded " + swipeCounter + " swipes. Do you want to proceed to the next activity?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // User confirms to proceed
                    Intent intent = new Intent(DataCollectionActivity.this, TrainingActivity.class);
                    intent.putStringArrayListExtra("RAW_SWIPE_DATA", new ArrayList<>(rawSwipeData));
                    intent.putStringArrayListExtra("STATISTICAL_SWIPE_DATA", new ArrayList<>(statisticalSwipeData));
                    startActivity(intent);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // User cancels the operation
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    private void shareData() {
        try {
            File rawDataFile = new File(getExternalFilesDir(null), userName + "_RAW.csv");
            File statisticalDataFile = new File(getExternalFilesDir(null), userName + "_STA.csv");

            writeListToFile(rawSwipeData, rawDataFile);
            writeListToFile(statisticalSwipeData, statisticalDataFile);

            ArrayList<Uri> uris = new ArrayList<>();
            uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", rawDataFile));
            uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", statisticalDataFile));

            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("text/csv");
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share CSV files"));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sharing data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writeListToFile(List<String> data, File file) throws IOException {
        FileWriter writer = new FileWriter(file);
        for (String line : data) {
            writer.write(line + "\n");
        }
        writer.close();
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
                pathLength = 0;
                directionChanges = 0;
                lastAngle = 0;
                float jerkX = 0;
                float jerkY = 0;
                curvature = 0;
                initialVelocityX = 0;
                initialVelocityY = 0;
                String initialData = formatData(swipeCounter, movementId, pointerId, time, x, y, pressure, size,
                        0, 0, 0, 0, 0, 0, 0, orientation, pathLength, 0, 0, 0, 0, 0, 0, 0);
                swipeData.add(initialData);
                rawSwipeData.add(initialData);
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

                // Update path length
                pathLength += distance;

                // Calculate jerk
                jerkX = (accelerationX - (0.0f) / (time - lastTime)) / (time - lastTime);
                jerkY = (accelerationY - (0.0f) / (time - lastTime)) / (time - lastTime);

                // Update direction changes
                if (movementId > 1 && Math.abs(angle - lastAngle) > 45) {
                    directionChanges++;
                }
                lastAngle = angle;

                // Calculate curvature (approximation)
                if (distance > 0) {
                    curvature = Math.abs(angle - lastAngle) / distance;
                }

                // Update initial and final velocities
                if (movementId == 1) {
                    initialVelocityX = velocityX;
                    initialVelocityY = velocityY;
                }

                String moveData = formatData(swipeCounter, movementId, pointerId, time, x, y, pressure, size,
                        velocityX, velocityY, accelerationX, accelerationY, angle, duration, distance, orientation,
                        pathLength, initialVelocityX, initialVelocityY, velocityX, velocityY,
                        directionChanges, curvature, (float) Math.sqrt(jerkX * jerkX + jerkY * jerkY));
                swipeData.add(moveData);
                rawSwipeData.add(moveData);
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
                calculateAndSaveStatistics();
                break;
        }
        return true;
    }

    private void calculateAndSaveStatistics() {
        DescriptiveStatistics xStats = new DescriptiveStatistics();
        DescriptiveStatistics yStats = new DescriptiveStatistics();
        DescriptiveStatistics pressureStats = new DescriptiveStatistics();
        DescriptiveStatistics sizeStats = new DescriptiveStatistics();
        DescriptiveStatistics velocityXStats = new DescriptiveStatistics();
        DescriptiveStatistics velocityYStats = new DescriptiveStatistics();
        DescriptiveStatistics accelerationXStats = new DescriptiveStatistics();
        DescriptiveStatistics accelerationYStats = new DescriptiveStatistics();
        DescriptiveStatistics angleStats = new DescriptiveStatistics();
        DescriptiveStatistics distanceStats = new DescriptiveStatistics();
        DescriptiveStatistics curvatureStats = new DescriptiveStatistics();
        DescriptiveStatistics jerkStats = new DescriptiveStatistics();

        int startIndex = rawSwipeData.size() - movementId;
        for (int i = startIndex; i < rawSwipeData.size(); i++) {
            String[] values = rawSwipeData.get(i).split(",");
            xStats.addValue(Double.parseDouble(values[4]));
            yStats.addValue(Double.parseDouble(values[5]));
            pressureStats.addValue(Double.parseDouble(values[6]));
            sizeStats.addValue(Double.parseDouble(values[7]));
            velocityXStats.addValue(Double.parseDouble(values[8]));
            velocityYStats.addValue(Double.parseDouble(values[9]));
            accelerationXStats.addValue(Double.parseDouble(values[10]));
            accelerationYStats.addValue(Double.parseDouble(values[11]));
            angleStats.addValue(Double.parseDouble(values[12]));
            distanceStats.addValue(Double.parseDouble(values[14]));
            curvatureStats.addValue(Double.parseDouble(values[22]));
            jerkStats.addValue(Double.parseDouble(values[23]));
        }

        String[] lastValues = rawSwipeData.get(rawSwipeData.size() - 1).split(",");
        float straightLineDistance = (float) Math.sqrt(
                Math.pow(xStats.getMax() - xStats.getMin(), 2) +
                        Math.pow(yStats.getMax() - yStats.getMin(), 2));

        String statisticalData = String.format(Locale.US,
                "%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f," +
                        "%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f," +
                        "%.2f,%.2f,%s,%s,%s,%s,%s,%s,%s,%.2f,%.2f",
                swipeCounter - 1,
                xStats.getMean(), xStats.getStandardDeviation(), xStats.getMin(), xStats.getMax(),
                yStats.getMean(), yStats.getStandardDeviation(), yStats.getMin(), yStats.getMax(),
                pressureStats.getMean(), pressureStats.getMax(), pressureStats.getStandardDeviation(),
                sizeStats.getMean(), sizeStats.getMax(),
                velocityXStats.getMean(), velocityXStats.getMax(), velocityXStats.getStandardDeviation(),
                velocityYStats.getMean(), velocityYStats.getMax(), velocityYStats.getStandardDeviation(),
                accelerationXStats.getMean(), accelerationXStats.getMax(), accelerationXStats.getStandardDeviation(),
                accelerationYStats.getMean(), accelerationYStats.getMax(), accelerationYStats.getStandardDeviation(),
                angleStats.getMean(), angleStats.getStandardDeviation(),
                distanceStats.getSum(), straightLineDistance,
                lastValues[16], lastValues[17], lastValues[18], lastValues[19], lastValues[20],
                lastValues[13], lastValues[21],
                curvatureStats.getMean(), jerkStats.getMean()
        );

        statisticalSwipeData.add(statisticalData);
    }

    private String formatData(int index, int movementID, int pointerId, long time, float x, float y, float pressure, float size,
                              float velocityX, float velocityY, float accelerationX, float accelerationY,
                              float angle, long duration, float distance, int orientation, float pathLength,
                              float initialVelocityX, float initialVelocityY, float finalVelocityX, float finalVelocityY,
                              int directionChanges, float curvature, float jerkMagnitude) {
        return String.format(Locale.US,
                "%d,%d,%d,%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%d,%f,%d,%f,%f,%f,%f,%f,%d,%f,%f",
                index, movementID, pointerId, time, x, y, pressure, size, velocityX, velocityY,
                accelerationX, accelerationY, angle, duration, distance, orientation, pathLength,
                initialVelocityX, initialVelocityY, finalVelocityX, finalVelocityY,
                directionChanges, curvature, jerkMagnitude);
    }

}
