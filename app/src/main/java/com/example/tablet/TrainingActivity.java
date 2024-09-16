package com.example.tablet;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import weka.classifiers.misc.IsolationForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.File;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrainingActivity extends AppCompatActivity {

    private static final String TAG = "TrainingActivity";
    private ProgressBar progressBar;
    private TextView progressText;
    private static final double CONTAMINATION_RATE = 0.05;
    private ArrayList<String> testData;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_training);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        progressBar = findViewById(R.id.trainingProgressBar);
        progressText = findViewById(R.id.progressText);

        ArrayList<String> statisticalSwipeData = getIntent().getStringArrayListExtra("STATISTICAL_SWIPE_DATA");

        if (statisticalSwipeData != null && !statisticalSwipeData.isEmpty()) {
            trainIsolationForest(statisticalSwipeData);
        } else {
            progressText.setText("No data received for training.");
        }
    }

    @SuppressLint("SetTextI18n")
    private void trainIsolationForest(ArrayList<String> statisticalSwipeData) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                String[] header = statisticalSwipeData.get(0).split(",");
                ArrayList<String> data = new ArrayList<>(statisticalSwipeData.subList(1, statisticalSwipeData.size()));

                // Splitting data into training and testing sets (80% train, 20% test)
                int trainSize = (int) (data.size() * 0.8);
                ArrayList<String> trainingData = new ArrayList<>(data.subList(0, trainSize));
                testData = new ArrayList<>(data.subList(trainSize, data.size()));

                ArrayList<Attribute> attributes = new ArrayList<>();

                for (int i = 1; i < header.length; i++) {
                    attributes.add(new Attribute(header[i]));
                }

                // Add dummy class attribute
                ArrayList<String> classValues = new ArrayList<>(Arrays.asList("normal", "anomaly"));
                attributes.add(new Attribute("class", classValues));

                Instances dataset = new Instances("SwipeData", attributes, trainingData.size());
                dataset.setClassIndex(dataset.numAttributes() - 1);

                for (String dataPoint : trainingData) {
                    String[] values = dataPoint.split(",");
                    double[] instanceValues = new double[attributes.size()];

                    for (int i = 1; i < values.length; i++) {
                        instanceValues[i - 1] = Double.parseDouble(values[i]);
                    }
                    // Set dummy class value (always "normal" for training data)
                    instanceValues[instanceValues.length - 1] = dataset.attribute(dataset.numAttributes() - 1).indexOfValue("normal");

                    dataset.add(new DenseInstance(1.0, instanceValues));
                }

                int subsampleSize = calculateSubsampleSize(dataset.size());

                IsolationForest isoForest = new IsolationForest();
                isoForest.setNumTrees(100);
                isoForest.setSubsampleSize(subsampleSize);

                for (int i = 0; i < 100; i++) {
                    int progress = i + 1;
                    handler.post(() -> {
                        progressBar.setProgress(progress);
                        progressText.setText("Training Progress: " + progress + "%");
                    });
                    Thread.sleep(50);
                }

                isoForest.buildClassifier(dataset);

                File modelFile = new File(getFilesDir(), "isolation_forest_model.model");
                try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(modelFile.toPath()))) {
                    oos.writeObject(isoForest);
                }

                handler.post(() -> {
                    String results = "Isolation Forest Training Completed\n" +
                            "Number of trees: " + isoForest.getNumTrees() + "\n" +
                            "Subsample size: " + subsampleSize + "\n" +
                            "Contamination rate: " + (CONTAMINATION_RATE * 100) + "%\n" +
                            "Total instances trained on: " + dataset.numInstances() + "\n" +
                            "Number of attributes: " + (dataset.numAttributes() - 1) + "\n" +
                            "Test instances: " + testData.size();
                    progressText.setText(results);

                    showCompletionDialog(modelFile.getAbsolutePath());
                });

            } catch (Exception e) {
                Log.e(TAG, "Error training Isolation Forest: " + e.getMessage());
                handler.post(() -> progressText.setText("Error training model: " + e.getMessage()));
            }
        });
    }

    private void showCompletionDialog(String modelFilePath) {
        new AlertDialog.Builder(this)
                .setTitle("Training Complete")
                .setMessage("Training has been completed. Do you want to proceed to the testing activity?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(TrainingActivity.this, TestingActivity.class);
                    intent.putExtra("MODEL_FILE_PATH", modelFilePath);
                    intent.putStringArrayListExtra("TEST_DATA", testData);
                    startActivity(intent);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // User chose not to proceed, do nothing or handle as needed
                })
                .setCancelable(false)
                .show();
    }

    private int calculateSubsampleSize(int datasetSize) {
        return Math.max(1, Math.min(datasetSize, (int) (datasetSize * (1 - CONTAMINATION_RATE))));
    }
}