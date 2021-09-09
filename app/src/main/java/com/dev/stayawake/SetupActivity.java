package com.dev.stayawake;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class SetupActivity extends Activity {
    TextView alarm_point_txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

        new AlertDialog.Builder(this).setMessage(R.string.sensitivity_alert_dialog).
                setPositiveButton(R.string.ok, null).show();

        Button apply_btn = findViewById(R.id.apply_btn);
        Button close_btn = findViewById(R.id.close_btn);
        alarm_point_txt= findViewById(R.id.alarmpoint_txt);
        SeekBar eyes_seekBar = findViewById(R.id.eyes_seekbar);
        eyes_seekBar.setProgress(Constants.ALARM_POINT);
        alarm_point_txt.setText(Constants.ALARM_POINT + "");

        close_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    finish();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }catch (Exception ignored) {
                }
            }
        });

        apply_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    SaveData.getInstance(getApplicationContext()).setAlarmPoint(Constants.ALARM_POINT);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } catch (Exception ignored) {
                }
            }
        });
        eyes_seekBar.incrementProgressBy(1);

        SeekBar.OnSeekBarChangeListener SeekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //add code here
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //add code here
            }

            @Override
            public void onProgressChanged(SeekBar seekBark, int progress, boolean fromUser) {
                //add code here
                //Log.w(TAG, "eyes_degree -> " + progress);
                Constants.ALARM_POINT = progress;
                alarm_point_txt.setText(Constants.ALARM_POINT + "");
            }
        };
        eyes_seekBar.setOnSeekBarChangeListener(SeekBarListener);
    }
}

