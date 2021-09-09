package com.dev.stayawake;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Objects;

public class AppInfoActivity extends AppCompatActivity {

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(this.getPackageName(), 0);
            SaveData.getInstance(getApplicationContext()).setAppVersion(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException ignored) {

        }
        String versionName = Objects.requireNonNull(pInfo).versionName;
        Log.i("release name",versionName);

        TextView version = findViewById(R.id.version);
        version.setText("Version : " + versionName);

        Button close_btn = findViewById(R.id.close_btn);
        close_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    finish();
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
