package ru.kheynov.wearback.fixture;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** An independent app/task with a real back stack, packaged only in the test APK. */
public class BackStackFixtureActivity extends Activity {
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        int depth = getIntent().getIntExtra("depth", 0);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        TextView title = new TextView(this);
        title.setText("WearBack fixture depth " + depth);
        title.setGravity(Gravity.CENTER);
        content.addView(title);
        Button next = new Button(this);
        next.setText("Next test screen");
        next.setOnClickListener(view -> startActivity(
                new Intent(this, BackStackFixtureActivity.class).putExtra("depth", depth + 1)));
        content.addView(next);
        setContentView(content);
    }
}
