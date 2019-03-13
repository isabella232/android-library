/* Copyright Urban Airship and Contributors */

package com.urbanairship.push;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.support.annotation.XmlRes;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.urbanairship.AirshipComponent;
import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.AirshipExecutors;
import com.urbanairship.Logger;
import com.urbanairship.PreferenceDataStore;
import com.urbanairship.R;
import com.urbanairship.UAirship;
import com.urbanairship.job.JobDispatcher;
import com.urbanairship.job.JobInfo;
import com.urbanairship.json.JsonException;
import com.urbanairship.json.JsonList;
import com.urbanairship.json.JsonValue;
import com.urbanairship.locale.LocaleManager;
import com.urbanairship.push.notifications.AirshipNotificationProvider;
import com.urbanairship.push.notifications.LegacyNotificationFactoryProvider;
import com.urbanairship.push.notifications.NotificationActionButtonGroup;
import com.urbanairship.push.notifications.NotificationChannelCompat;
import com.urbanairship.push.notifications.NotificationChannelRegistry;
import com.urbanairship.push.notifications.NotificationFactory;
import com.urbanairship.push.notifications.NotificationProvider;
import com.urbanairship.util.UAStringUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * This class is the primary interface for customizing the display and behavior
 * of incoming push notifications.
 */
public class PushManager extends AirshipComponent {

    /**
     * Action sent as a broadcast when a push message is received.
     * <p>
     * Extras:
     * {@link #EXTRA_NOTIFICATION_ID},
     * {@link #EXTRA_PUSH_MESSAGE_BUNDLE}
     *
     * @hide
     */
    @NonNull
    public static final String ACTION_PUSH_RECEIVED = "com.urbanairship.push.RECEIVED";

    /**
     * Action sent as a broadcast when a notification is opened.
     * <p>
     * Extras:
     * {@link #EXTRA_NOTIFICATION_ID},
     * {@link #EXTRA_PUSH_MESSAGE_BUNDLE},
     * {@link #EXTRA_NOTIFICATION_BUTTON_ID},
     * {@link #EXTRA_NOTIFICATION_BUTTON_FOREGROUND}
     *
     * @hide
     */
    @NonNull
    public static final String ACTION_NOTIFICATION_OPENED = "com.urbanairship.push.OPENED";

    /**
     * Action sent as a broadcast when a notification is dismissed.
     * <p>
     * Extras:
     * {@link #EXTRA_NOTIFICATION_ID},
     * {@link #EXTRA_PUSH_MESSAGE_BUNDLE}
     *
     * @hide
     */
    @NonNull
    public static final String ACTION_NOTIFICATION_DISMISSED = "com.urbanairship.push.DISMISSED";

    /**
     * Action sent as a broadcast when a channel registration succeeds.
     * <p>
     * Extras:
     * {@link #EXTRA_CHANNEL_ID}
     *
     * @hide
     */
    @NonNull
    public static final String ACTION_CHANNEL_UPDATED = "com.urbanairship.push.CHANNEL_UPDATED";

    /**
     * The notification ID extra contains the ID of the notification placed in the
     * <code>NotificationManager</code> by the library.
     * <p>
     * If a <code>Notification</code> was not created, the extra will not be included.
     */
    @NonNull
    public static final String EXTRA_NOTIFICATION_ID = "com.urbanairship.push.NOTIFICATION_ID";

    /**
     * The notification tag extra contains the tag of the notification placed in the
     * <code>NotificationManager</code> by the library.
     * <p>
     * If a <code>Notification</code> was not created, the extra will not be included.
     */
    @NonNull
    public static final String EXTRA_NOTIFICATION_TAG = "com.urbanairship.push.NOTIFICATION_TAG";

    /**
     * The push message extra bundle.
     *
     * @hide
     */
    @NonNull
    public static final String EXTRA_PUSH_MESSAGE_BUNDLE = "com.urbanairship.push.EXTRA_PUSH_MESSAGE_BUNDLE";

    /**
     * The interactive notification action button identifier extra.
     *
     * @hide
     */
    @NonNull
    public static final String EXTRA_NOTIFICATION_BUTTON_ID = "com.urbanairship.push.EXTRA_NOTIFICATION_BUTTON_ID";

    /**
     * The flag indicating if the interactive notification action button is background or foreground.
     *
     * @hide
     */
    @NonNull
    public static final String EXTRA_NOTIFICATION_BUTTON_FOREGROUND = "com.urbanairship.push.EXTRA_NOTIFICATION_BUTTON_FOREGROUND";

    /**
     * Extra used to indicate an error in channel registration.
     *
     * @hide
     */
    @NonNull
    public static final String EXTRA_ERROR = "com.urbanairship.push.EXTRA_ERROR";

    /**
     * The channel ID extra.
     *
     * @hide
     */
    @NonNull
    public static final String EXTRA_CHANNEL_ID = "com.urbanairship.push.EXTRA_CHANNEL_ID";

    /**
     * Extra used to indicate whether a channel registration request type is create or update.
     *
     * @hide
     */
    @NonNull
    public static final String EXTRA_CHANNEL_CREATE_REQUEST = "com.urbanairship.push.EXTRA_CHANNEL_CREATE_REQUEST";

    /**
     * This intent action indicates that a push notification has been opened.
     *
     * @hide
     */
    @NonNull
    public static final String ACTION_NOTIFICATION_OPENED_PROXY = "com.urbanairship.ACTION_NOTIFICATION_OPENED_PROXY";

    /**
     * This intent action indicates that a push notification button has been opened.
     *
     * @hide
     */
    @NonNull
    public static final String ACTION_NOTIFICATION_BUTTON_OPENED_PROXY = "com.urbanairship.ACTION_NOTIFICATION_BUTTON_OPENED_PROXY";

    /**
     * This intent action indicates that a push notification button has been dismissed.
     *
     * @hide
     */
    @NonNull
    public static final String ACTION_NOTIFICATION_DISMISSED_PROXY = "com.urbanairship.ACTION_NOTIFICATION_DISMISSED_PROXY";

    /**
     * The CONTENT_INTENT extra is an optional intent that the notification builder can
     * supply on the notification. If set, the intent will be pulled from the notification,
     * stored as part of the supplied UA intent, and then sent inside the UA core receiver.
     *
     * @hide
     */
    @NonNull
    public static final String EXTRA_NOTIFICATION_CONTENT_INTENT = "com.urbanairship.push.EXTRA_NOTIFICATION_CONTENT_INTENT";

    /**
     * The DELETE_INTENT extra is an optional intent that the notification builder can
     * supply on the notification. If set, the intent will be pulled from the notification,
     * stored as part of the supplied UA intent, and then sent inside the UA core receiver.
     *
     * @hide
     */
    @NonNull
    public static final String EXTRA_NOTIFICATION_DELETE_INTENT = "com.urbanairship.push.EXTRA_NOTIFICATION_DELETE_INTENT";

    /**
     * The description of the notification action button.
     *
     * @hide
     */
    @NonNull
    public static final String EXTRA_NOTIFICATION_ACTION_BUTTON_DESCRIPTION = "com.urbanairship.push.EXTRA_NOTIFICATION_ACTION_BUTTON_DESCRIPTION";

    /**
     * The actions payload for the notification action button.
     *
     * @hide
     */
    @NonNull
    public static final String EXTRA_NOTIFICATION_BUTTON_ACTIONS_PAYLOAD = "com.urbanairship.push.EXTRA_NOTIFICATION_BUTTON_ACTIONS_PAYLOAD";

    private final String UA_NOTIFICATION_BUTTON_GROUP_PREFIX = "ua_";

    /**
     * Key to store the push canonical IDs for push deduping.
     */
    private static final String LAST_CANONICAL_IDS_KEY = "com.urbanairship.push.LAST_CANONICAL_IDS";

    /**
     * Max amount of canonical IDs to store.
     */
    private static final int MAX_CANONICAL_IDS = 10;

    /**
     * The default tag group.
     */
    private final String DEFAULT_TAG_GROUP = "device";

    static final ExecutorService PUSH_EXECUTOR = AirshipExecutors.THREAD_POOL_EXECUTOR;

    static final String KEY_PREFIX = "com.urbanairship.push";
    static final String PUSH_ENABLED_KEY = KEY_PREFIX + ".PUSH_ENABLED";
    static final String USER_NOTIFICATIONS_ENABLED_KEY = KEY_PREFIX + ".USER_NOTIFICATIONS_ENABLED";
    static final String PUSH_TOKEN_REGISTRATION_ENABLED_KEY = KEY_PREFIX + ".PUSH_TOKEN_REGISTRATION_ENABLED";

    // As of version 5.0.0
    static final String PUSH_ENABLED_SETTINGS_MIGRATED_KEY = KEY_PREFIX + ".PUSH_ENABLED_SETTINGS_MIGRATED";
    static final String CHANNEL_LOCATION_KEY = KEY_PREFIX + ".CHANNEL_LOCATION";
    static final String CHANNEL_ID_KEY = KEY_PREFIX + ".CHANNEL_ID";
    static final String TAGS_KEY = KEY_PREFIX + ".TAGS";
    static final String LAST_RECEIVED_METADATA = KEY_PREFIX + ".LAST_RECEIVED_METADATA";


    static final String ADM_REGISTRATION_ID_KEY = KEY_PREFIX + ".ADM_REGISTRATION_ID_KEY";
    static final String GCM_INSTANCE_ID_TOKEN_KEY = KEY_PREFIX + ".GCM_INSTANCE_ID_TOKEN_KEY";
    static final String APID_KEY = KEY_PREFIX + ".APID";

    // As of version 8.0.0
    static final String REGISTRATION_TOKEN_KEY = KEY_PREFIX + ".REGISTRATION_TOKEN_KEY";
    static final String REGISTRATION_TOKEN_MIGRATED_KEY = KEY_PREFIX + ".REGISTRATION_TOKEN_MIGRATED_KEY";

    //singleton stuff
    private final Context context;
    private NotificationProvider notificationProvider;
    private final Map<String, NotificationActionButtonGroup> actionGroupMap = new HashMap<>();
    private boolean channelTagRegistrationEnabled = true;
    private final PreferenceDataStore preferenceDataStore;
    private final AirshipConfigOptions configOptions;
    private boolean channelCreationDelayEnabled;
    private final NotificationManagerCompat notificationManagerCompat;

    private final JobDispatcher jobDispatcher;
    private final TagGroupRegistrar tagGroupRegistrar;
    private final PushProvider pushProvider;
    private PushManagerJobHandler jobHandler;
    private NotificationChannelRegistry notificationChannelRegistry;

    private final Object tagLock = new Object();

    /**
     * Creates a PushManager. Normally only one push manager instance should exist, and
     * can be accessed from {@link com.urbanairship.UAirship#getPushManager()}.
     *
     * @param context Application context
     * @param preferenceDataStore The preferences data store.
     * @param configOptions The airship config options.
     * @param pushProvider The push provider.
     * @hide
     */
    public PushManager(@NonNull Context context, @NonNull PreferenceDataStore preferenceDataStore,
                       @NonNull AirshipConfigOptions configOptions, @Nullable PushProvider pushProvider,
                       @NonNull TagGroupRegistrar tagGroupRegistrar) {

        this(context, preferenceDataStore, configOptions, pushProvider, tagGroupRegistrar, JobDispatcher.shared(context));
    }

    /**
     * @hide
     */
    @VisibleForTesting
    PushManager(@NonNull Context context, @NonNull PreferenceDataStore preferenceDataStore,
                @NonNull AirshipConfigOptions configOptions, PushProvider provider,
                @NonNull TagGroupRegistrar tagGroupRegistrar, @NonNull JobDispatcher dispatcher) {
        super(context, preferenceDataStore);
        this.context = context;
        this.preferenceDataStore = preferenceDataStore;
        this.jobDispatcher = dispatcher;
        this.pushProvider = provider;
        this.tagGroupRegistrar = tagGroupRegistrar;
        this.notificationProvider = new AirshipNotificationProvider(context, configOptions);
        this.configOptions = configOptions;
        this.notificationManagerCompat = NotificationManagerCompat.from(context);
        this.notificationChannelRegistry = new NotificationChannelRegistry(context, configOptions);

        this.actionGroupMap.putAll(ActionButtonGroupsParser.fromXml(context, R.xml.ua_notification_buttons));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.actionGroupMap.putAll(ActionButtonGroupsParser.fromXml(context, R.xml.ua_notification_button_overrides));
        }
    }

    @Override
    protected void init() {
        super.init();
        if (Logger.getLogLevel() < Log.ASSERT && !UAStringUtil.isEmpty(getChannelId())) {
            Log.d(UAirship.getAppName() + " Channel ID", getChannelId());
        }

        this.migratePushEnabledSettings();
        this.migrateRegistrationTokenSettings();

        channelCreationDelayEnabled = getChannelId() == null && configOptions.channelCreationDelayEnabled;

        // Start registration
        if (UAirship.isMainProcess()) {
            JobInfo jobInfo = JobInfo.newBuilder()
                                     .setAction(PushManagerJobHandler.ACTION_UPDATE_PUSH_REGISTRATION)
                                     .setId(JobInfo.CHANNEL_UPDATE_PUSH_TOKEN)
                                     .setAirshipComponent(PushManager.class)
                                     .build();

            jobDispatcher.dispatch(jobInfo);

            // If we have a channel already check for pending tags
            if (getChannelId() != null) {
                dispatchUpdateTagGroupsJob();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onComponentEnableChange(boolean isEnabled) {
        if (isEnabled) {
            JobInfo jobInfo = JobInfo.newBuilder()
                                     .setAction(PushManagerJobHandler.ACTION_UPDATE_PUSH_REGISTRATION)
                                     .setId(JobInfo.CHANNEL_UPDATE_PUSH_TOKEN)
                                     .setAirshipComponent(PushManager.class)
                                     .build();

            jobDispatcher.dispatch(jobInfo);
        }
    }

    /**
     * @hide
     */
    @NonNull
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Executor getJobExecutor(@NonNull JobInfo jobInfo) {
        if (jobInfo.getAction().equals(PushManagerJobHandler.ACTION_PROCESS_PUSH)) {
            return PUSH_EXECUTOR;
        }

        return super.getJobExecutor(jobInfo);
    }

    /**
     * @hide
     */
    @WorkerThread
    @JobInfo.JobResult
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public int onPerformJob(@NonNull UAirship airship, @NonNull JobInfo jobInfo) {
        if (jobHandler == null) {
            jobHandler = new PushManagerJobHandler(context, airship, preferenceDataStore, tagGroupRegistrar);
        }

        return jobHandler.performJob(jobInfo);
    }

    /**
     * Enables channel creation if channel creation has been delayed.
     * <p>
     * This setting is persisted between application starts, so there is no need to call this
     * repeatedly. It is only necessary to call this when channelCreationDelayEnabled has been
     * set to <code>true</code> in the airship config.
     */
    public void enableChannelCreation() {
        if (isChannelCreationDelayEnabled()) {
            channelCreationDelayEnabled = false;
            updateRegistration();
        }
    }

    /**
     * Enables or disables push notifications.
     * <p>
     * This setting is persisted between application starts, so there is no need to call this
     * repeatedly. It is only necessary to call this when a user preference has changed.
     *
     * @param enabled A boolean indicating whether push is enabled.
     */
    public void setPushEnabled(boolean enabled) {
        preferenceDataStore.put(PUSH_ENABLED_KEY, enabled);
        updateRegistration();
    }

    /**
     * Determines whether push is enabled.
     *
     * @return <code>true</code> if push is enabled, <code>false</code> otherwise.
     * This defaults to true, and must be explicitly set by the app.
     */
    public boolean isPushEnabled() {
        return preferenceDataStore.getBoolean(PUSH_ENABLED_KEY, true);
    }

    /**
     * Enables or disables user notifications.
     * <p>
     * User notifications are push notifications that contain an alert message and are
     * intended to be shown to the user.
     * <p>
     * This setting is persisted between application starts, so there is no need to call this
     * repeatedly. It is only necessary to call this when a user preference has changed.
     *
     * @param enabled A boolean indicating whether user push is enabled.
     */
    public void setUserNotificationsEnabled(boolean enabled) {
        preferenceDataStore.put(USER_NOTIFICATIONS_ENABLED_KEY, enabled);
        updateRegistration();
    }

    /**
     * Determines whether user-facing push notifications are enabled.
     *
     * @return <code>true</code> if user push is enabled, <code>false</code> otherwise.
     */
    public boolean getUserNotificationsEnabled() {
        return preferenceDataStore.getBoolean(USER_NOTIFICATIONS_ENABLED_KEY, false);
    }

    /**
     * Sets the notification factory used when push notifications are received.
     * <p>
     * Specify a notification factory here to customize the display
     * of a push notification's Custom Expanded Views in the
     * Android Notification Manager.
     * <p>
     * If <code>null</code>, push notifications will not be displayed by the
     * library.
     *
     * @param factory The notification factory
     * @see com.urbanairship.push.notifications.NotificationFactory
     * @see com.urbanairship.push.notifications.DefaultNotificationFactory
     * @see com.urbanairship.push.notifications.CustomLayoutNotificationFactory
     * @deprecated Use {@link com.urbanairship.push.notifications.NotificationProvider} and {@link #setNotificationProvider(NotificationProvider)}
     * instead. Marked to be removed in SDK 11.
     */
    public void setNotificationFactory(@Nullable NotificationFactory factory) {
        if (factory == null) {
            this.notificationProvider = null;
        } else {
            this.notificationProvider = new LegacyNotificationFactoryProvider(factory);
        }
    }

    /**
     * Sets the notification provider used to build notifications from a push message
     *
     * If <code>null</code>, notification will not be displayed.
     *
     * @param provider The notification provider
     * @see com.urbanairship.push.notifications.NotificationProvider
     * @see com.urbanairship.push.notifications.AirshipNotificationProvider
     * @see com.urbanairship.push.notifications.CustomLayoutNotificationProvider
     */
    public void setNotificationProvider(@Nullable NotificationProvider provider) {
        this.notificationProvider = provider;
    }

    /**
     * Gets the notification provider.
     *
     * @return The notification provider.
     */
    @Nullable
    public NotificationProvider getNotificationProvider() {
        return notificationProvider;
    }

    /**
     * Returns the <code>PreferenceDataStore</code> singleton for this application.
     *
     * @return The PreferenceDataStore
     */
    @NonNull
    PreferenceDataStore getPreferenceDataStore() {
        return preferenceDataStore;
    }

    /**
     * Returns the shared notification channel registry.
     *
     * @return The NotificationChannelRegistry
     */
    @NonNull
    public NotificationChannelRegistry getNotificationChannelRegistry() {
        return notificationChannelRegistry;
    }

    /**
     * Determines whether the app is capable of receiving push,
     * meaning whether a GCM or ADM registration ID is present.
     *
     * @return <code>true</code> if push is available, <code>false</code> otherwise.
     */
    public boolean isPushAvailable() {
        return getPushTokenRegistrationEnabled() && !UAStringUtil.isEmpty(getRegistrationToken());
    }

    /**
     * Returns if the application is currently opted in for push.
     *
     * @return <code>true</code> if opted in for push.
     */
    public boolean isOptIn() {
        return isPushEnabled() && isPushAvailable() && areNotificationsOptedIn();
    }

    /**
     * Checks if notifications are enabled for the app and in the push manager.
     *
     * @return {@code true} if notifications are opted in, otherwise {@code false}.
     */
    public boolean areNotificationsOptedIn() {
        return getUserNotificationsEnabled() && notificationManagerCompat.areNotificationsEnabled();
    }

    /**
     * Returns the next channel registration payload
     *
     * @return The ChannelRegistrationPayload payload
     */
    @NonNull
    ChannelRegistrationPayload getNextChannelRegistrationPayload() {
        ChannelRegistrationPayload.Builder builder = new ChannelRegistrationPayload.Builder()
                .setTags(getChannelTagRegistrationEnabled(), getTags())
                .setOptIn(isOptIn())
                .setBackgroundEnabled(isPushEnabled() && isPushAvailable())
                .setUserId(UAirship.shared().getInbox().getUser().getId())
                .setApid(getApid());

        switch (UAirship.shared().getPlatformType()) {
            case UAirship.ANDROID_PLATFORM:
                builder.setDeviceType("android");
                break;
            case UAirship.AMAZON_PLATFORM:
                builder.setDeviceType("amazon");
                break;
        }

        builder.setTimezone(TimeZone.getDefault().getID());

        Locale locale = LocaleManager.shared(context).getDefaultLocale();

        if (!UAStringUtil.isEmpty(locale.getCountry())) {
            builder.setCountry(locale.getCountry());
        }

        if (!UAStringUtil.isEmpty(locale.getLanguage())) {
            builder.setLanguage(locale.getLanguage());
        }

        if (getPushTokenRegistrationEnabled()) {
            builder.setPushAddress(getRegistrationToken());
        }

        return builder.build();
    }

    /**
     * Update registration.
     */
    public void updateRegistration() {
        JobInfo jobInfo = JobInfo.newBuilder()
                                 .setAction(PushManagerJobHandler.ACTION_UPDATE_CHANNEL_REGISTRATION)
                                 .setId(JobInfo.CHANNEL_UPDATE_REGISTRATION)
                                 .setNetworkAccessRequired(true)
                                 .setAirshipComponent(PushManager.class)
                                 .build();

        jobDispatcher.dispatch(jobInfo);
    }

    /**
     * Set tags for the channel and update the server.
     * <p>
     * Tags should be URL-safe with a length greater than 0 and less than 127 characters. If your
     * tag includes whitespace or special characters, we recommend URL encoding the string.
     * <p>
     * To clear the current set of tags, pass an empty set to this method.
     *
     * @param tags A set of tag strings.
     */
    public void setTags(@NonNull Set<String> tags) {
        //noinspection ConstantConditions
        if (tags == null) {
            throw new IllegalArgumentException("Tags must be non-null.");
        }

        synchronized (tagLock) {
            Set<String> normalizedTags = TagUtils.normalizeTags(tags);
            preferenceDataStore.put(TAGS_KEY, JsonValue.wrapOpt(normalizedTags));
        }

        updateRegistration();
    }

    /**
     * Returns the current set of tags.
     * <p>
     * An empty set indicates that no tags are set on this channel.
     *
     * @return The current set of tags.
     */
    @NonNull
    public Set<String> getTags() {
        synchronized (tagLock) {
            Set<String> tags = new HashSet<>();

            JsonValue jsonValue = preferenceDataStore.getJsonValue(TAGS_KEY);

            if (jsonValue.isJsonList()) {
                for (JsonValue tag : jsonValue.optList()) {
                    if (tag.isString()) {
                        tags.add(tag.getString());
                    }
                }
            }

            Set<String> normalizedTags = TagUtils.normalizeTags(tags);

            //to prevent the getTags call from constantly logging tag set failures, sync tags
            if (tags.size() != normalizedTags.size()) {
                this.setTags(normalizedTags);
            }

            return normalizedTags;
        }
    }

    /**
     * Determines whether tags are enabled on the device.
     * If <code>false</code>, no locally specified tags will be sent to the server during registration.
     * The default value is <code>true</code>.
     *
     * @return <code>true</code> if tags are enabled on the device, <code>false</code> otherwise.
     */
    public boolean getChannelTagRegistrationEnabled() {
        return channelTagRegistrationEnabled;
    }

    /**
     * Sets whether tags are enabled on the device. The default value is <code>true</code>.
     * If <code>false</code>, no locally specified tags will be sent to the server during registration.
     *
     * @param enabled A boolean indicating whether tags are enabled on the device.
     */
    public void setChannelTagRegistrationEnabled(boolean enabled) {
        channelTagRegistrationEnabled = enabled;
    }

    /**
     * Determines whether the GCM token or ADM ID is sent during channel registration.
     * If {@code false}, the app will not be able to receive push notifications.
     * The default value is {@code true}.
     *
     * @return {@code true} if the GCM token or ADM ID is sent during channel registration,
     * {@code false} otherwise.
     */
    public boolean getPushTokenRegistrationEnabled() {
        return preferenceDataStore.getBoolean(PUSH_TOKEN_REGISTRATION_ENABLED_KEY, true);
    }

    /**
     * Sets whether the GCM token or ADM ID is sent during channel registration.
     * If {@code false}, the app will not be able to receive push notifications.
     *
     * @param enabled A boolean indicating whether the GCM token or ADM ID is sent during
     * channel registration.
     */
    public void setPushTokenRegistrationEnabled(boolean enabled) {
        preferenceDataStore.put(PUSH_TOKEN_REGISTRATION_ENABLED_KEY, enabled);
        updateRegistration();
    }


    /**
     * Determines whether channel creation is initially disabled, to be enabled later
     * by enableChannelCreation.
     *
     * @return <code>true</code> if channel creation is initially disabled, <code>false</code> otherwise.
     */
    boolean isChannelCreationDelayEnabled() {
        return channelCreationDelayEnabled;
    }

    /**
     * Returns the send metadata of the last received push.
     *
     * @return The send metadata from the last received push, or null if not found.
     */
    @Nullable
    public String getLastReceivedMetadata() {
        return preferenceDataStore.getString(LAST_RECEIVED_METADATA, null);
    }

    /**
     * Store the send metadata from the last received push.
     *
     * @param sendMetadata The send metadata string.
     */
    void setLastReceivedMetadata(String sendMetadata) {
        preferenceDataStore.put(LAST_RECEIVED_METADATA, sendMetadata);
    }

    /**
     * Edit the channel tag groups.
     *
     * @return A {@link TagGroupsEditor}.
     */
    @NonNull
    public TagGroupsEditor editTagGroups() {
        return new TagGroupsEditor() {
            @Override
            protected boolean allowTagGroupChange(@NonNull String tagGroup) {
                if (channelTagRegistrationEnabled && DEFAULT_TAG_GROUP.equals(tagGroup)) {
                    Logger.error("Unable to add tags to `device` tag group when `channelTagRegistrationEnabled` is true.");
                    return false;
                }

                return true;
            }

            @Override
            protected void onApply(@NonNull List<TagGroupsMutation> collapsedMutations) {

                if (collapsedMutations.isEmpty()) {
                    return;
                }

                tagGroupRegistrar.addMutations(TagGroupRegistrar.CHANNEL, collapsedMutations);

                if (getChannelId() != null) {
                    dispatchUpdateTagGroupsJob();
                }
            }
        };
    }

    /**
     * Edits channel Tags.
     *
     * @return A {@link TagEditor}
     */
    @NonNull
    public TagEditor editTags() {
        return new TagEditor() {
            @Override
            void onApply(boolean clear, @NonNull Set<String> tagsToAdd, @NonNull Set<String> tagsToRemove) {
                synchronized (tagLock) {
                    Set<String> tags = clear ? new HashSet<String>() : getTags();

                    tags.addAll(tagsToAdd);
                    tags.removeAll(tagsToRemove);

                    setTags(tags);
                }
            }
        };
    }

    /**
     * Register a notification action group under the given name.
     * <p>
     * The provided notification builders will automatically add the actions to the
     * notification when a message is received with a group specified under the
     * {@link com.urbanairship.push.PushMessage#EXTRA_INTERACTIVE_TYPE}
     * key.
     *
     * @param id The id of the action group.
     * @param group The notification action group.
     */
    public void addNotificationActionButtonGroup(@NonNull String id, @NonNull NotificationActionButtonGroup group) {
        if (id.startsWith(UA_NOTIFICATION_BUTTON_GROUP_PREFIX)) {
            Logger.error("Unable to add any notification button groups that starts with the reserved Urban Airship prefix %s", UA_NOTIFICATION_BUTTON_GROUP_PREFIX);
            return;
        }

        actionGroupMap.put(id, group);
    }

    /**
     * Adds notification action button groups from an xml file.
     * Example entry:
     * <pre>{@code
     * <UrbanAirshipActionButtonGroup id="custom_group">
     *  <UrbanAirshipActionButton
     *      foreground="true"
     *      id="yes"
     *      android:icon="@drawable/ua_ic_notification_button_accept"
     *      android:label="@string/ua_notification_button_yes"/>
     *  <UrbanAirshipActionButton
     *      foreground="false"
     *      id="no"
     *      android:icon="@drawable/ua_ic_notification_button_decline"
     *      android:label="@string/ua_notification_button_no"/>
     * </UrbanAirshipActionButtonGroup> }</pre>
     *
     * @param context The application context.
     * @param resId The xml resource ID.
     */
    public void addNotificationActionButtonGroups(@NonNull Context context, @XmlRes int resId) {
        Map<String, NotificationActionButtonGroup> groups = ActionButtonGroupsParser.fromXml(context, resId);
        for (Map.Entry<String, NotificationActionButtonGroup> entry : groups.entrySet()) {
            addNotificationActionButtonGroup(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes the notification button group under the given name.
     *
     * @param id The id of the button group to remove.
     */
    public void removeNotificationActionButtonGroup(@NonNull String id) {
        if (id.startsWith(UA_NOTIFICATION_BUTTON_GROUP_PREFIX)) {
            Logger.error("Unable to remove any reserved Urban Airship actions groups that begin with %s", UA_NOTIFICATION_BUTTON_GROUP_PREFIX);
            return;
        }

        actionGroupMap.remove(id);
    }

    /**
     * Returns the notification action group that is registered under the given name.
     *
     * @param id The id of the action group.
     * @return The notification action group.
     */
    @Nullable
    public NotificationActionButtonGroup getNotificationActionGroup(@Nullable String id) {
        if (id == null) {
            return null;
        }
        return actionGroupMap.get(id);
    }

    /**
     * Get the Channel ID
     *
     * @return A Channel ID string
     */
    @Nullable
    public String getChannelId() {
        return preferenceDataStore.getString(CHANNEL_ID_KEY, null);
    }

    /**
     * Gets the channel location.
     *
     * @return The channel location.
     */
    @Nullable
    String getChannelLocation() {
        return preferenceDataStore.getString(CHANNEL_LOCATION_KEY, null);
    }

    /**
     * Sets the Channel ID and channel location.
     * Also update the user.
     *
     * @param channelId The channel ID as a string.
     * @param channelLocation The channel location as a URL.
     */
    void setChannel(String channelId, String channelLocation) {
        preferenceDataStore.put(CHANNEL_ID_KEY, channelId);
        preferenceDataStore.put(CHANNEL_LOCATION_KEY, channelLocation);
    }

    /**
     * Dispatches a job to update the tag groups.
     */
    void dispatchUpdateTagGroupsJob() {
        JobInfo jobInfo = JobInfo.newBuilder()
                                 .setAction(PushManagerJobHandler.ACTION_UPDATE_TAG_GROUPS)
                                 .setId(JobInfo.CHANNEL_UPDATE_TAG_GROUPS)
                                 .setNetworkAccessRequired(true)
                                 .setAirshipComponent(PushManager.class)
                                 .build();

        jobDispatcher.dispatch(jobInfo);
    }

    /**
     * Sets the GCM Instance ID or ADM ID token.
     *
     * @param token The GCM Instance ID or ADM ID token.
     */
    void setRegistrationToken(String token) {
        preferenceDataStore.put(REGISTRATION_TOKEN_KEY, token);
    }

    /**
     * Gets the GCM Instance ID or ADM ID token.
     *
     * @return The GCM or ADM token.
     */
    @Nullable
    public String getRegistrationToken() {
        return preferenceDataStore.getString(REGISTRATION_TOKEN_KEY, null);
    }

    /**
     * Return the device's existing APID
     *
     * @return an APID string or null if it doesn't exist.
     */
    @Nullable
    String getApid() {
        return preferenceDataStore.getString(APID_KEY, null);
    }

    /**
     * Migrates the old push enabled setting to the new user notifications enabled
     * setting, and enables push by default. This was introduced in version 5.0.0.
     */
    void migratePushEnabledSettings() {

        if (preferenceDataStore.getBoolean(PUSH_ENABLED_SETTINGS_MIGRATED_KEY, false)) {
            return;
        }

        Logger.debug("Migrating push enabled preferences");

        // get old push enabled value, defaulting to false as before
        boolean oldPushEnabled = this.preferenceDataStore.getBoolean(PUSH_ENABLED_KEY, false);

        // copy old push enabled value to user notifications enabled slot
        Logger.debug("Setting user notifications enabled to %s", Boolean.toString(oldPushEnabled));
        preferenceDataStore.put(USER_NOTIFICATIONS_ENABLED_KEY, oldPushEnabled);

        if (!oldPushEnabled) {
            Logger.info("Push is now enabled. You can continue to toggle the opt-in state by enabling or disabling user notifications");
        }

        // set push enabled to true
        preferenceDataStore.put(PUSH_ENABLED_KEY, true);
        preferenceDataStore.put(PUSH_ENABLED_SETTINGS_MIGRATED_KEY, true);
    }

    /**
     * Migrates the stored registration token.
     */
    void migrateRegistrationTokenSettings() {
        if (preferenceDataStore.getBoolean(REGISTRATION_TOKEN_MIGRATED_KEY, false)) {
            return;
        }

        Logger.debug("Migrating registration token preference");

        String token = null;
        switch (UAirship.shared().getPlatformType()) {
            case UAirship.ANDROID_PLATFORM:
                token = preferenceDataStore.getString(GCM_INSTANCE_ID_TOKEN_KEY, null);
                break;
            case UAirship.AMAZON_PLATFORM:
                token = preferenceDataStore.getString(ADM_REGISTRATION_ID_KEY, null);
                break;
        }

        if (!UAStringUtil.isEmpty(token)) {
            setRegistrationToken(token);
        }

        preferenceDataStore.put(REGISTRATION_TOKEN_MIGRATED_KEY, true);
    }

    /**
     * Gets the push provider.
     *
     * @return The available push provider.
     */
    @Nullable
    PushProvider getPushProvider() {
        return pushProvider;
    }

    /**
     * Check to see if we've seen this ID before. If we have,
     * return false. If not, add the ID to our history and return true.
     *
     * @param canonicalId The canonical push ID for an incoming notification.
     * @return <code>false</code> if the ID exists in the history, otherwise <code>true</code>.
     */
    boolean isUniqueCanonicalId(@Nullable String canonicalId) {
        if (UAStringUtil.isEmpty(canonicalId)) {
            return true;
        }

        JsonList jsonList = null;
        try {
            jsonList = JsonValue.parseString(preferenceDataStore.getString(LAST_CANONICAL_IDS_KEY, null)).getList();
        } catch (JsonException e) {
            Logger.debug(e, "PushJobHandler - Unable to parse canonical Ids.");
        }

        List<JsonValue> canonicalIds = jsonList == null ? new ArrayList<JsonValue>() : jsonList.getList();

        // Wrap the canonicalId
        JsonValue id = JsonValue.wrap(canonicalId);

        // Check if the list contains the canonicalId
        if (canonicalIds.contains(id)) {
            return false;
        }

        // Add it
        canonicalIds.add(id);
        if (canonicalIds.size() > MAX_CANONICAL_IDS) {
            canonicalIds = canonicalIds.subList(canonicalIds.size() - MAX_CANONICAL_IDS, canonicalIds.size());
        }

        // Store the new list
        preferenceDataStore.put(LAST_CANONICAL_IDS_KEY, JsonValue.wrapOpt(canonicalIds).toString());

        return true;
    }

}
