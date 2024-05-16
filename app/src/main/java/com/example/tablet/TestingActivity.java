package com.example.tablet;

import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Random;

public class TestingActivity extends AppCompatActivity {

    private ImageView imageViewCheck;
    private ImageView imageViewCross;
    private GestureDetector gestureDetector;
    private Random random = new Random();
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_testing);

        imageViewCheck = findViewById(R.id.imageViewCheck);
        imageViewCross = findViewById(R.id.imageViewCross);

        gestureDetector = new GestureDetector(this, new GestureListener());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            displayRandomIcon();
            return true;
        }
    }

    private void displayRandomIcon() {
        boolean showCheckIcon = random.nextInt(100) < 90;
        imageViewCheck.setVisibility(showCheckIcon ? ImageView.VISIBLE : ImageView.GONE);
        imageViewCross.setVisibility(showCheckIcon ? ImageView.GONE : ImageView.VISIBLE);

        // Hide the icon after 3 seconds
        handler.postDelayed(() -> {
            imageViewCheck.setVisibility(ImageView.GONE);
            imageViewCross.setVisibility(ImageView.GONE);
        }, 1500);  //milliseconds
    }
}
