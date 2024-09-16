package com.example.tablet;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.classifiers.misc.IsolationForest;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Locale;

public class TestingActivity extends AppCompatActivity {
    private static final String TAG = "TestingActivity";
    private ImageView imageViewCheck;
    private ImageView imageViewCross;
    private TextView resultTextView;
    private IsolationForest model;
    private ArrayList<String> swipeData;
    private float lastX, lastY;
    private long lastTime, initialTime;
    private float lastVelocityX, lastVelocityY;
    private int movementId;
    private float pathLength;
    private int directionChanges;
    private float lastAngle;
    private float jerkX, jerkY;
    private float curvature;
    private float initialVelocityX, initialVelocityY;
    private float finalVelocityX, finalVelocityY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_testing);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        initializeViews();
        loadModel(getIntent().getStringExtra("MODEL_FILE_PATH"));
        swipeData = new ArrayList<>();
    }

    @SuppressLint("SetTextI18n")
    private void initializeViews() {
        imageViewCheck = findViewById(R.id.imageViewCheck);
        imageViewCross = findViewById(R.id.imageViewCross);
        resultTextView = findViewById(R.id.resultTextView);

        if (imageViewCheck != null) imageViewCheck.setVisibility(View.GONE);
        if (imageViewCross != null) imageViewCross.setVisibility(View.GONE);
        if (resultTextView != null) resultTextView.setText("Swipe to test");
    }

    @SuppressLint("SetTextI18n")
    private void loadModel(String modelFilePath) {
        try {
            FileInputStream fis = new FileInputStream(modelFilePath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            model = (IsolationForest) ois.readObject();
            ois.close();
            fis.close();
        } catch (Exception e) {
            Log.e(TAG, "Error loading model: " + e.getMessage());
            if (resultTextView != null) {
                resultTextView.setText("Error loading model: " + e.getMessage());
            }
        }
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

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initializeViews();
                initializeSwipeData(x, y, time);
                break;
            case MotionEvent.ACTION_MOVE:
                collectSwipeData(x, y, time, pressure, size, pointerId);
                break;
            case MotionEvent.ACTION_UP:
                finalizeAndTestSwipe();
                break;
        }
        return true;
    }

    private void initializeSwipeData(float x, float y, long time) {
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
        jerkX = 0;
        jerkY = 0;
        curvature = 0;
        initialVelocityX = 0;
        initialVelocityY = 0;
        swipeData.clear();  // Clear previous swipe data
    }

    private void collectSwipeData(float x, float y, long time, float pressure, float size, int pointerId) {
        float velocityX = (x - lastX) / (time - lastTime);
        float velocityY = (y - lastY) / (time - lastTime);
        float accelerationX = (velocityX - lastVelocityX) / (time - lastTime);
        float accelerationY = (velocityY - lastVelocityY) / (time - lastTime);
        float angle = (float) Math.toDegrees(Math.atan2(y - lastY, x - lastX));
        float distance = (float) Math.sqrt(Math.pow(x - lastX, 2) + Math.pow(y - lastY, 2));
        long duration = time - initialTime;

        pathLength += distance;

        jerkX = (accelerationX - 0.0f) / (time - lastTime);
        jerkY = (accelerationY - 0.0f) / (time - lastTime);

        if (movementId > 1 && Math.abs(angle - lastAngle) > 45) {
            directionChanges++;
        }
        lastAngle = angle;

        if (distance > 0) {
            curvature = Math.abs(angle - lastAngle) / distance;
        }

        if (movementId == 1) {
            initialVelocityX = velocityX;
            initialVelocityY = velocityY;
        }
        finalVelocityX = velocityX;
        finalVelocityY = velocityY;

        String moveData = formatData(movementId, pointerId, time, x, y, pressure, size,
                velocityX, velocityY, accelerationX, accelerationY, angle, duration, distance,
                getResources().getConfiguration().orientation, pathLength, initialVelocityX, initialVelocityY,
                finalVelocityX, finalVelocityY, directionChanges, curvature,
                (float) Math.sqrt(jerkX * jerkX + jerkY * jerkY));
        swipeData.add(moveData);
        movementId++;

        lastX = x;
        lastY = y;
        lastTime = time;
        lastVelocityX = velocityX;
        lastVelocityY = velocityY;
    }

    private void finalizeAndTestSwipe() {
        if (swipeData.isEmpty()) {
            Log.e(TAG, "No swipe data collected");
            displayResult(false);
            return;
        }

        try {
            double[] features = calculateStatistics();
            boolean isAuthentic = testSwipe(features);
            displayResult(isAuthentic);
        } catch (Exception e) {
            Log.e(TAG, "Error in finalizeAndTestSwipe: " + e.getMessage());
            e.printStackTrace();
            displayResult(false);
        }
    }

    private double[] calculateStatistics() {
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

        for (String data : swipeData) {
            String[] values = data.split(",");
            if (values.length < 23) {  // Changed from 24 to 23 as we removed the index column
                Log.e(TAG, "Invalid data format: " + data);
                continue;
            }
            try {
                xStats.addValue(Double.parseDouble(values[3]));  // Shifted index by 1
                yStats.addValue(Double.parseDouble(values[4]));
                pressureStats.addValue(Double.parseDouble(values[5]));
                sizeStats.addValue(Double.parseDouble(values[6]));
                velocityXStats.addValue(Double.parseDouble(values[7]));
                velocityYStats.addValue(Double.parseDouble(values[8]));
                accelerationXStats.addValue(Double.parseDouble(values[9]));
                accelerationYStats.addValue(Double.parseDouble(values[10]));
                angleStats.addValue(Double.parseDouble(values[11]));
                distanceStats.addValue(Double.parseDouble(values[13]));
                curvatureStats.addValue(Double.parseDouble(values[21]));
                jerkStats.addValue(Double.parseDouble(values[22]));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing values: " + e.getMessage());
            }
        }

        if (swipeData.isEmpty()) {
            Log.e(TAG, "No valid swipe data to calculate statistics");
            return new double[39]; // Return an array of zeros
        }

        String[] lastValues = swipeData.get(swipeData.size() - 1).split(",");
        float straightLineDistance = (float) Math.sqrt(
                Math.pow(xStats.getMax() - xStats.getMin(), 2) +
                        Math.pow(yStats.getMax() - yStats.getMin(), 2));

        return new double[]{
                xStats.getMean(), xStats.getStandardDeviation(), xStats.getMin(), xStats.getMax(),
                yStats.getMean(), yStats.getStandardDeviation(), yStats.getMin(), yStats.getMax(),
                pressureStats.getMean(), pressureStats.getMax(), pressureStats.getStandardDeviation(),
                sizeStats.getMean(), sizeStats.getMax(),
                velocityXStats.getMean(), velocityXStats.getMax(), velocityXStats.getStandardDeviation(),
                velocityYStats.getMean(), velocityYStats.getMax(), velocityYStats.getStandardDeviation(),
                accelerationXStats.getMean(), accelerationXStats.getMax(), accelerationXStats.getStandardDeviation(),
                accelerationYStats.getMean(), accelerationYStats.getMax(), accelerationYStats.getStandardDeviation(),
                angleStats.getMean(), angleStats.getStandardDeviation(),
                distanceStats.getSum(), straightLineDistance, pathLength,
                lastValues.length > 16 ? Double.parseDouble(lastValues[16]) : 0,
                lastValues.length > 17 ? Double.parseDouble(lastValues[17]) : 0,
                lastValues.length > 18 ? Double.parseDouble(lastValues[18]) : 0,
                lastValues.length > 19 ? Double.parseDouble(lastValues[19]) : 0,
                lastValues.length > 12 ? Double.parseDouble(lastValues[12]) : 0,
                lastValues.length > 20 ? Double.parseDouble(lastValues[20]) : 0,
                curvatureStats.getMean(), jerkStats.getMean()
        };
    }

    private boolean testSwipe(double[] features) {
        try {
            ArrayList<Attribute> attributes = new ArrayList<>();
            for (int i = 0; i < features.length; i++) {
                attributes.add(new Attribute("attr" + i));
            }

            // Add a numeric class attribute
            attributes.add(new Attribute("class"));

            Instances dataUnlabeled = new Instances("TestInstances", attributes, 1);
            dataUnlabeled.setClassIndex(dataUnlabeled.numAttributes() - 1);

            double[] instanceValue = new double[dataUnlabeled.numAttributes()];
            System.arraycopy(features, 0, instanceValue, 0, features.length);
            instanceValue[instanceValue.length - 1] = weka.core.Utils.missingValue(); // Set class to missing

            weka.core.Instance instance = new DenseInstance(1.0, instanceValue);
            instance.setDataset(dataUnlabeled);

            if (model == null) {
                Log.e(TAG, "Model is not initialized");
                return false;
            }

            // Use distributionForInstance instead of classifyInstance
            double[] distribution = model.distributionForInstance(instance);
            double anomalyScore = distribution[0]; // The anomaly score is in the first element
            Log.d(TAG, "Isolation Forest anomaly score: " + anomalyScore);

            // In Weka's IsolationForest, lower scores indicate more anomalous instances
            // You may need to adjust this threshold based on your model's behavior
            return anomalyScore > 0.45; // Adjust this threshold as needed
        } catch (Exception e) {
            Log.e(TAG, "Error in testSwipe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void displayResult(boolean isAuthentic) {
        runOnUiThread(() -> {
            if (imageViewCheck != null) {
                imageViewCheck.setVisibility(isAuthentic ? View.VISIBLE : View.GONE);
            }
            if (imageViewCross != null) {
                imageViewCross.setVisibility(isAuthentic ? View.GONE : View.VISIBLE);
            }
            if (resultTextView != null) {
                resultTextView.setText(isAuthentic ? "Authentic Swipe" : "Unauthentic Swipe");
            }
            Log.d(TAG, "Swipe authentication result: " + (isAuthentic ? "Authentic" : "Unauthentic"));
        });
    }

    private String formatData(int movementID, int pointerId, long time, float x, float y, float pressure, float size,
                              float velocityX, float velocityY, float accelerationX, float accelerationY,
                              float angle, long duration, float distance, int orientation, float pathLength,
                              float initialVelocityX, float initialVelocityY, float finalVelocityX, float finalVelocityY,
                              int directionChanges, float curvature, float jerkMagnitude) {
        return String.format(Locale.US,
                "%d,%d,%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%d,%f,%d,%f,%f,%f,%f,%f,%d,%f,%f",
                movementID, pointerId, time, x, y, pressure, size, velocityX, velocityY,
                accelerationX, accelerationY, angle, duration, distance, orientation, pathLength,
                initialVelocityX, initialVelocityY, finalVelocityX, finalVelocityY,
                directionChanges, curvature, jerkMagnitude);
    }
}