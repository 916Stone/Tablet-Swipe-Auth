package com.example.tablet;

import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Random;

public class TrainingActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView progressText;
    private Handler handler = new Handler();
    private Random random = new Random();
    private int currentProgress = 0; // Track the cumulative progress across phases

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_training);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        progressBar = findViewById(R.id.trainingProgressBar);
        progressText = findViewById(R.id.progressText);

        startTrainingProcess();
    }

    private void startTrainingProcess() {
        new Thread(() -> {
            executePhase("Processing data...", 13, 16, 33);
            executePhase("Extracting features...", 18, 22, 66);
            executePhase("Training model...", 4, 6, 100);
            handler.post(this::showCompletionDialog);
        }).start();
    }

    private void executePhase(String phaseText, int minSeconds, int maxSeconds, int progressTarget) {
        int duration = random.nextInt((maxSeconds - minSeconds + 1) + minSeconds) * 1000; // Duration in milliseconds
        long endTime = System.currentTimeMillis() + duration;
        int startProgress = currentProgress; // Start from the last progress
        handler.post(() -> progressText.setText(phaseText));

        while (System.currentTimeMillis() < endTime) {
            long timeRemaining = endTime - System.currentTimeMillis();
            int progressIncrement = (int) (((progressTarget - startProgress) * (1 - timeRemaining / (double) duration)));
            currentProgress = startProgress + progressIncrement;
            handler.post(() -> progressBar.setProgress(currentProgress));
            try {
                Thread.sleep(100);  // Update interval
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        currentProgress = progressTarget; // Ensure it exactly reaches the target at the end
        handler.post(() -> progressBar.setProgress(currentProgress));
    }

    private void showCompletionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Training complete");
        builder.setMessage("Training complete, do you wish to proceed to the test?");
        builder.setPositiveButton("Yes", (dialog, which) -> proceedToNextActivity());
        builder.setCancelable(false);  // Prevent dismiss by tapping outside of the dialog
        builder.show();
    }

    private void proceedToNextActivity() {
        Intent intent = new Intent(this, TestingActivity.class);
        startActivity(intent);
    }
}
