/* Copyright 2018 Urban Airship and Contributors */

package com.urbanairship.push;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.Logger;
import com.urbanairship.UAirship;
import com.urbanairship.http.RequestFactory;
import com.urbanairship.http.Response;
import com.urbanairship.json.JsonException;
import com.urbanairship.json.JsonMap;
import com.urbanairship.json.JsonValue;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * TagGroup API client.
 */
class TagGroupApiClient extends BaseApiClient {

    private static final String CHANNEL_TAGS_PATH = "api/channels/tags/";
    private static final String NAMED_USER_TAG_GROUP_PATH = "api/named_users/tags/";

    private static final String ANDROID_CHANNEL_KEY = "android_channel";
    private static final String AMAZON_CHANNEL_KEY = "amazon_channel";
    private static final String NAMED_USER_ID_KEY = "named_user_id";

    private static final String AUDIENCE_KEY = "audience";

    TagGroupApiClient(@UAirship.Platform int platform, @NonNull AirshipConfigOptions configOptions) {
        this(platform, configOptions, RequestFactory.DEFAULT_REQUEST_FACTORY);
    }

    @VisibleForTesting
    TagGroupApiClient(@UAirship.Platform int platform, @NonNull AirshipConfigOptions configOptions, @NonNull RequestFactory requestFactory) {
        super(platform, configOptions, requestFactory);
    }

    /**
     * Update the tag groups for the given identifier.
     *
     * @param audienceId The audienceId.
     * @param mutation The tag group mutation.
     *
     * @return The response or null if an error occurred.
     */
    Response updateTagGroups(@TagGroupRegistrar.TagGroupType int type, @NonNull String audienceId, @NonNull TagGroupsMutation mutation) {
        URL tagUrl = getDeviceUrl(type == TagGroupRegistrar.NAMED_USER ? NAMED_USER_TAG_GROUP_PATH : CHANNEL_TAGS_PATH);
        if (tagUrl == null) {
            Logger.error("Invalid tag URL. Unable to update tagGroups.");
            return null;
        }

        JsonMap payload = JsonMap.newBuilder()
                                 .putAll(mutation.toJsonValue().optMap())
                                 .put(AUDIENCE_KEY, JsonMap.newBuilder()
                                                           .put(getTagGroupAudienceSelector(type), audienceId)
                                                           .build())
                                 .build();


        String tagPayload = payload.toString();
        Logger.info("Updating tag groups with payload: " + tagPayload);

        Response response = performRequest(tagUrl, "POST", tagPayload);
        logTagGroupResponseIssues(response);

        return response;
    }

    /**
     * Log the response warnings and errors if they exist in the response body.
     *
     * @param response The tag group response.
     */
    private void logTagGroupResponseIssues(Response response) {
        if (response == null || response.getResponseBody() == null) {
            return;
        }

        String responseBody = response.getResponseBody();

        JsonValue responseJson;
        try {
            responseJson = JsonValue.parseString(responseBody);
        } catch (JsonException e) {
            Logger.error("Unable to parse tag group response", e);
            return;
        }

        if (responseJson.isJsonMap()) {
            // Check for any warnings in the response and log them if they exist.
            if (responseJson.getMap().containsKey("warnings")) {
                for (JsonValue warning : responseJson.getMap().get("warnings").getList()) {
                    Logger.warn("Tag Groups warnings: " + warning);
                }
            }

            // Check for any errors in the response and log them if they exist.
            if (responseJson.getMap().containsKey("error")) {
                Logger.error("Tag Groups error: " + responseJson.getMap().get("error"));
            }
        }
    }

    private String getTagGroupAudienceSelector(@TagGroupRegistrar.TagGroupType int type) {
        switch (type) {
            case TagGroupRegistrar.CHANNEL:
                switch (this.getPlatform()) {
                    case UAirship.AMAZON_PLATFORM:
                        return AMAZON_CHANNEL_KEY;

                    case UAirship.ANDROID_PLATFORM:
                    default:
                        return ANDROID_CHANNEL_KEY;
                }
            case TagGroupRegistrar.NAMED_USER:
                return NAMED_USER_ID_KEY;
        }

        throw new IllegalArgumentException("Unknown type: " + type);
    }
}
