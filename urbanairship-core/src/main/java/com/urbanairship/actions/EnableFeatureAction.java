/* Copyright Airship and Contributors */

package com.urbanairship.actions;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import com.urbanairship.Logger;
import com.urbanairship.PrivacyManager;
import com.urbanairship.UAirship;
import com.urbanairship.base.Supplier;
import com.urbanairship.modules.location.AirshipLocationClient;
import com.urbanairship.util.Checks;
import com.urbanairship.util.HelperActivity;
import com.urbanairship.util.PermissionsRequester;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

/**
 * An action that enables features. Running the action with value {@link #FEATURE_LOCATION} or {@link #FEATURE_BACKGROUND_LOCATION}
 * will prompt the user for permissions before enabling.
 * <p>
 * Accepted situations: SITUATION_PUSH_OPENED, SITUATION_WEB_VIEW_INVOCATION,
 * SITUATION_MANUAL_INVOCATION, SITUATION_AUTOMATION, and SITUATION_FOREGROUND_NOTIFICATION_ACTION_BUTTON.
 * <p>
 * Accepted argument value - either {@link #FEATURE_USER_NOTIFICATIONS}, {@link #FEATURE_BACKGROUND_LOCATION},
 * or {@link #FEATURE_LOCATION}.
 * <p>
 * Result value: {@code true} if the feature was enabled. {@code false} if the feature required user
 * permissions that were rejected by the user.
 * <p>
 * Default Registration Names: {@link #DEFAULT_REGISTRY_NAME}, {@link #DEFAULT_REGISTRY_SHORT_NAME}
 */
public class EnableFeatureAction extends Action {

    /**
     * Default registry name
     */
    @NonNull
    public static final String DEFAULT_REGISTRY_NAME = "enable_feature";

    /**
     * Default registry short name
     */
    @NonNull
    public static final String DEFAULT_REGISTRY_SHORT_NAME = "^ef";

    /**
     * Action value to enable user notifications. See {@link com.urbanairship.push.PushManager#setUserNotificationsEnabled(boolean)}
     */
    @NonNull
    public static final String FEATURE_USER_NOTIFICATIONS = "user_notifications";

    /**
     * Action value to enable location.
     */
    @NonNull
    public static final String FEATURE_LOCATION = "location";

    /**
     * Action value to enable location with background updates.
     */
    @NonNull
    public static final String FEATURE_BACKGROUND_LOCATION = "background_location";

    private final PermissionsRequester permissionsRequester;
    private final Supplier<UAirship> airship;

    public EnableFeatureAction(@NonNull PermissionsRequester permissionsRequester,
                               @NonNull Supplier<UAirship> airship) {
        this.permissionsRequester = permissionsRequester;
        this.airship = airship;

    }

    public EnableFeatureAction() {
        this(new PermissionsRequester() {
            @NonNull
            @Override
            public int[] requestPermissions(@NonNull Context context, @NonNull List<String> permissions) {
                return HelperActivity.requestPermissions(context, permissions.toArray(new String[0]));
            }
        }, new Supplier<UAirship>() {
            @Override
            public UAirship get() {
                return UAirship.shared();
            }
        });
    }

    @Override
    public boolean acceptsArguments(@NonNull ActionArguments arguments) {

        // Validate situation
        switch (arguments.getSituation()) {
            case SITUATION_PUSH_OPENED:
            case SITUATION_WEB_VIEW_INVOCATION:
            case SITUATION_MANUAL_INVOCATION:
            case SITUATION_FOREGROUND_NOTIFICATION_ACTION_BUTTON:
            case SITUATION_AUTOMATION:
                break;

            case SITUATION_PUSH_RECEIVED:
            case SITUATION_BACKGROUND_NOTIFICATION_ACTION_BUTTON:
            default:
                return false;
        }

        // Validate value
        String feature = arguments.getValue().getString();
        if (feature == null) {
            return false;
        }

        switch (feature) {
            case FEATURE_BACKGROUND_LOCATION:
            case FEATURE_LOCATION:
            case FEATURE_USER_NOTIFICATIONS:
                return true;
            default:
                return false;
        }
    }

    @NonNull
    @Override
    public ActionResult perform(@NonNull ActionArguments arguments) {
        String feature = arguments.getValue().getString();
        Checks.checkNotNull(feature, "Missing feature.");
        AirshipLocationClient locationClient = airship.get().getLocationClient();
        PrivacyManager privacyManager = airship.get().getPrivacyManager();

        switch (feature) {
            case FEATURE_BACKGROUND_LOCATION:
                if (locationClient == null) {
                    return ActionResult.newEmptyResult();
                }

                privacyManager.enable(PrivacyManager.FEATURE_LOCATION);
                if (requestLocationPermissions()) {
                    locationClient.setLocationUpdatesEnabled(true);
                    locationClient.setBackgroundLocationAllowed(true);
                    return ActionResult.newResult(ActionValue.wrap(true));
                }

                return ActionResult.newResult(ActionValue.wrap(false));
            case FEATURE_LOCATION:
                if (locationClient == null) {
                    return ActionResult.newEmptyResult();
                }

                privacyManager.enable(PrivacyManager.FEATURE_LOCATION);
                if (requestLocationPermissions()) {
                    locationClient.setLocationUpdatesEnabled(true);
                    return ActionResult.newResult(ActionValue.wrap(true));
                }

                return ActionResult.newResult(ActionValue.wrap(false));
            case FEATURE_USER_NOTIFICATIONS:
                airship.get().getPushManager().setUserNotificationsEnabled(true);
                privacyManager.enable(PrivacyManager.FEATURE_PUSH);
                if (!NotificationManagerCompat.from(UAirship.getApplicationContext()).areNotificationsEnabled()) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            navigateToNotificationSettings();
                        }
                    });
                }
                return ActionResult.newResult(ActionValue.wrap(true));
        }

        return ActionResult.newResult(ActionValue.wrap(false));
    }

    private boolean requestLocationPermissions() {
        int[] result = permissionsRequester.requestPermissions(UAirship.getApplicationContext(), Arrays.asList(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION));
        for (int i : result) {
            if (i == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Navigates to the app notification settings screen.
     */
    @MainThread
    private static void navigateToNotificationSettings() {
        Context context = UAirship.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, UAirship.getPackageName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                context.startActivity(intent);
                return;
            } catch (ActivityNotFoundException e) {
                Logger.debug(e, "Failed to launch notification settings.");
            }
        }

        Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS")
                .putExtra("app_package", UAirship.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("app_uid", UAirship.getAppInfo().uid);

        try {
            context.startActivity(intent);
            return;
        } catch (ActivityNotFoundException e) {
            Logger.debug(e, "Failed to launch notification settings.");
        }

        intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(Uri.parse("package:" + UAirship.getPackageName()));

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Logger.error(e, "Unable to launch settings activity.");
        }
    }

}
