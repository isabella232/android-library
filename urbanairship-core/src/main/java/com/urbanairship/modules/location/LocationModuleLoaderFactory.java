/* Copyright Airship and Contributors */

package com.urbanairship.modules.location;

import android.content.Context;

import com.urbanairship.PreferenceDataStore;
import com.urbanairship.analytics.Analytics;
import com.urbanairship.channel.AirshipChannel;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Location module loader factory.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface LocationModuleLoaderFactory {
    LocationModuleLoader build(@NonNull Context context,
                               @NonNull PreferenceDataStore dataStore,
                               @NonNull AirshipChannel airshipChannel,
                               @NonNull Analytics analytics);
}
