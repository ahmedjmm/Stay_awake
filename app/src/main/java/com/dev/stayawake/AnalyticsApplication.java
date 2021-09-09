package com.dev.stayawake;

/*
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.google.android.gms.analytics.Tracker;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import com.google.android.gms.analytics.GoogleAnalytics;

/**
 * This is a subclass of {@link Application} used to provide shared objects for this app, such as
 * the {@link Tracker}.
 */
public class AnalyticsApplication extends Application {
    public static final String dismiss = "dismiss";
    private Tracker mTracker;
    public NotificationManager notificationManager;
    public static final String camera_service_notification_channel_id = "cameraServiceNotificationChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        getDefaultTracker();
        createNotificationChannel();
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(getApplicationContext());
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel cameraServiceNotification = new NotificationChannel(camera_service_notification_channel_id, "camera service", NotificationManager.IMPORTANCE_HIGH);
            cameraServiceNotification.setDescription("notification for running camera in background");
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(cameraServiceNotification);
        }
    }
}