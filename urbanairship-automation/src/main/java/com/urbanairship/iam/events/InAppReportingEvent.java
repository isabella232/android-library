/* Copyright Airship and Contributors */

package com.urbanairship.iam.events;

import com.urbanairship.analytics.Analytics;
import com.urbanairship.analytics.Event;
import com.urbanairship.android.layout.reporting.FormData;
import com.urbanairship.android.layout.reporting.PagerData;
import com.urbanairship.android.layout.reporting.LayoutData;
import com.urbanairship.iam.InAppMessage;
import com.urbanairship.iam.ResolutionInfo;
import com.urbanairship.json.JsonMap;
import com.urbanairship.json.JsonValue;

import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;

/**
 * In-app automation reporting.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InAppReportingEvent {

    // Event types
    @NonNull
    public static final String TYPE_RESOLUTION = "in_app_resolution";
    @NonNull
    public static final String TYPE_DISPLAY = "in_app_display";
    @NonNull
    public static final String TYPE_PAGE_VIEW = "in_app_page_view";
    @NonNull
    public static final String TYPE_PAGE_SWIPE = "in_app_page_swipe";
    @NonNull
    public static final String TYPE_FORM_DISPLAY = "in_app_form_display";
    @NonNull
    public static final String TYPE_FORM_RESULT = "in_app_display";
    @NonNull
    public static final String TYPE_BUTTON_TAP = "in_app_button_tap";

    // Form keys
    private static final String FORM_ID = "form_identifier";
    private static final String FORM = "form";

    // Pager keys
    private static final String PAGER_ID = "pager_identifier";
    private static final String PAGER_INDEX = "page_index";
    private static final String PAGER_COUNT = "page_count";
    private static final String PAGER_COMPLETED = "completed";
    private static final String PAGER_TO_INDEX = "to_page_index";
    private static final String PAGER_FROM_INDEX = "from_page_index";

    // Button keys
    private static final String BUTTON_IDENTIFIER = "button_identifier";

    // Resolution keys
    private static final String RESOLUTION = "resolution";
    private static final String RESOLUTION_TYPE = "type";
    private static final String LEGACY_MESSAGE_REPLACED = "replaced";
    private static final String LEGACY_MESSAGE_DIRECT_OPEN = "direct_open";
    private static final String DISPLAY_TIME = "display_time";
    private static final String BUTTON_ID = "button_id";
    private static final String BUTTON_DESCRIPTION = "button_description";
    private static final String REPLACEMENT_ID = "replacement_id";

    // Common keys
    private static final String ID = "id";
    private static final String CONVERSION_SEND_ID = "conversion_send_id";
    private static final String CONVERSION_METADATA = "conversion_metadata";
    private static final String SOURCE = "source";
    private static final String CONTEXT = "context";
    private static final String LOCALE = "locale";

    // Context keys
    private static final String REPORTING_CONTEXT = "reporting_context";
    private static final String REPORTING_CONTEXT_FORM = "form";
    private static final String REPORTING_CONTEXT_FORM_ID = "identifier";
    private static final String REPORTING_CONTEXT_PAGER = "pager";
    private static final String REPORTING_CONTEXT_PAGER_ID = "identifier";
    private static final String REPORTING_CONTEXT_PAGER_INDEX = "index";
    private static final String REPORTING_CONTEXT_PAGER_COUNT = "count";
    private static final String REPORTING_CONTEXT_PAGER_COMPLETED = "completed";

    // ID keys
    private static final String MESSAGE_ID = "message_id";
    private static final String CAMPAIGNS = "campaigns";

    // Source
    private static final String SOURCE_URBAN_AIRSHIP = "urban-airship";
    private static final String SOURCE_APP_DEFINED = "app-defined";

    private final String type;
    private final String scheduleId;
    @InAppMessage.Source
    private final String source;
    private final Map<String, JsonValue> renderedLocale;

    private JsonValue campaigns;
    private JsonValue reportingContext;
    private LayoutData layoutState;
    private JsonMap overrides;

    private InAppReportingEvent(@NonNull String type, @NonNull String scheduleId, @NonNull InAppMessage message) {
        this.type = type;
        this.scheduleId = scheduleId;
        this.source = message.getSource();
        this.renderedLocale = message.getRenderedLocale();
    }

    private InAppReportingEvent(@NonNull String type, @NonNull String scheduleId, @NonNull @InAppMessage.Source String source) {
        this.type = type;
        this.scheduleId = scheduleId;
        this.source = source;
        this.renderedLocale = null;
    }

    public static InAppReportingEvent display(@NonNull String scheduleId, @NonNull InAppMessage message) {
        return new InAppReportingEvent(TYPE_DISPLAY, scheduleId, message);
    }

    public static InAppReportingEvent interrupted(@NonNull String scheduleId, @NonNull @InAppMessage.Source String source) {
        JsonMap resolutionData = resolutionData(ResolutionInfo.dismissed(), 0);
        return new InAppReportingEvent(TYPE_FORM_RESULT, scheduleId, source)
                .setOverrides(JsonMap.newBuilder().put(RESOLUTION, resolutionData).build());
    }

    public static InAppReportingEvent resolution(@NonNull String scheduleId,
                                                 @NonNull InAppMessage message,
                                                 long displayMilliseconds,
                                                 @NonNull ResolutionInfo resolutionInfo) {

        return new InAppReportingEvent(TYPE_RESOLUTION, scheduleId, message)
                .setOverrides(JsonMap.newBuilder().put(RESOLUTION, resolutionData(resolutionInfo, displayMilliseconds)).build());
    }

    public static InAppReportingEvent legacyReplaced(@NonNull String scheduleId, @NonNull String newId) {
        JsonMap resolutionInfo = JsonMap.newBuilder()
                                        .put(RESOLUTION_TYPE, LEGACY_MESSAGE_REPLACED)
                                        .put(REPLACEMENT_ID, newId)
                                        .build();

        return new InAppReportingEvent(TYPE_RESOLUTION, scheduleId, InAppMessage.SOURCE_LEGACY_PUSH)
                .setOverrides(JsonMap.newBuilder().put(RESOLUTION, resolutionInfo).build());
    }

    public static InAppReportingEvent legacyPushOpened(@NonNull String scheduleId) {
        JsonMap resolutionInfo = JsonMap.newBuilder()
                                        .put(RESOLUTION_TYPE, LEGACY_MESSAGE_DIRECT_OPEN)
                                        .build();

        return new InAppReportingEvent(TYPE_RESOLUTION, scheduleId, InAppMessage.SOURCE_LEGACY_PUSH)
                .setOverrides(JsonMap.newBuilder().put(RESOLUTION, resolutionInfo).build());
    }

    public static InAppReportingEvent formDisplay(@NonNull String scheduleId,
                                                  @NonNull InAppMessage message,
                                                  @NonNull String formId) {
        return new InAppReportingEvent(TYPE_FORM_DISPLAY, scheduleId, message)
                .setOverrides(JsonMap.newBuilder().put(FORM_ID, formId).build());
    }

    public static InAppReportingEvent formResult(@NonNull String scheduleId,
                                                 @NonNull InAppMessage message,
                                                 @NonNull FormData<?> formData) {

        return new InAppReportingEvent(TYPE_FORM_RESULT, scheduleId, message)
                .setOverrides(JsonMap.newBuilder().put(FORM, formData).build());
    }

    public static InAppReportingEvent buttonTap(@NonNull String scheduleId,
                                                @NonNull InAppMessage message,
                                                @NonNull String buttonId) {
        return new InAppReportingEvent(TYPE_BUTTON_TAP, scheduleId, message)
                .setOverrides(JsonMap.newBuilder().put(BUTTON_IDENTIFIER, buttonId).build());
    }

    public static InAppReportingEvent pageView(@NonNull String scheduleId,
                                               @NonNull InAppMessage message,
                                               @NonNull PagerData pagerData) {
        return new InAppReportingEvent(TYPE_PAGE_VIEW, scheduleId, message)
                .setOverrides(JsonMap.newBuilder()
                                     .put(PAGER_COMPLETED, pagerData.isCompleted())
                                     .put(PAGER_ID, pagerData.getIdentifier())
                                     .put(PAGER_COUNT, pagerData.getCount())
                                     .put(PAGER_INDEX, pagerData.getIndex())
                                     .build());
    }

    public static InAppReportingEvent pageSwipe(@NonNull String scheduleId,
                                                @NonNull InAppMessage message,
                                                @NonNull PagerData pagerData,
                                                int toIndex,
                                                int fromIndex) {

        return new InAppReportingEvent(TYPE_PAGE_SWIPE, scheduleId, message)
                .setOverrides(JsonMap.newBuilder()
                                     .put(PAGER_ID, pagerData.getIdentifier())
                                     .put(PAGER_TO_INDEX, toIndex)
                                     .put(PAGER_FROM_INDEX, fromIndex)
                                     .build());
    }

    public InAppReportingEvent setCampaigns(@Nullable JsonValue campaigns) {
        this.campaigns = campaigns;
        return this;
    }

    public InAppReportingEvent setLayoutData(@Nullable LayoutData layoutState) {
        this.layoutState = layoutState;
        return this;
    }

    public InAppReportingEvent setReportingContext(@Nullable JsonValue reportingContext) {
        this.reportingContext = reportingContext;
        return this;
    }

    private InAppReportingEvent setOverrides(JsonMap overrides) {
        this.overrides = overrides;
        return this;
    }

    @NonNull
    public void record(Analytics analytics) {
        boolean isAppDefined = InAppMessage.SOURCE_APP_DEFINED.equals(source);
        JsonMap.Builder builder = JsonMap.newBuilder()
                                         .put(ID, createEventId(scheduleId, source, campaigns))
                                         .put(SOURCE, isAppDefined ? SOURCE_APP_DEFINED : SOURCE_URBAN_AIRSHIP)
                                         .putOpt(CONVERSION_SEND_ID, analytics.getConversionSendId())
                                         .putOpt(CONVERSION_METADATA, analytics.getConversionMetadata())
                                         .put(CONTEXT, contextData(layoutState, reportingContext));

        if (renderedLocale != null) {
            builder.putOpt(LOCALE, renderedLocale);
        }

        if (this.overrides != null) {
            builder.putAll(overrides);
        }

        analytics.addEvent(new AnalyticsEvent(type, builder.build()));
    }

    private static JsonMap resolutionData(ResolutionInfo resolutionInfo, long displayMilliseconds) {
        displayMilliseconds = displayMilliseconds > 0 ? displayMilliseconds : 0;

        JsonMap.Builder resolutionDataBuilder = JsonMap.newBuilder()
                                                       .put(RESOLUTION_TYPE, resolutionInfo.getType())
                                                       .put(DISPLAY_TIME, Event.millisecondsToSecondsString(displayMilliseconds));

        if (ResolutionInfo.RESOLUTION_BUTTON_CLICK.equals(resolutionInfo.getType()) && resolutionInfo.getButtonInfo() != null) {
            String description = resolutionInfo.getButtonInfo().getLabel().getText();
            resolutionDataBuilder.put(BUTTON_ID, resolutionInfo.getButtonInfo().getId())
                                 .put(BUTTON_DESCRIPTION, description);
        }
        return resolutionDataBuilder.build();
    }

    private static JsonMap contextData(@Nullable LayoutData layoutState, @Nullable JsonValue reportingContext) {
        JsonMap.Builder contextBuilder = JsonMap.newBuilder()
                                                .put(REPORTING_CONTEXT, reportingContext);

        if (layoutState != null) {
            String formId = layoutState.getFormId();
            if (formId != null) {
                JsonMap formContext = JsonMap.newBuilder()
                                             .put(REPORTING_CONTEXT_FORM_ID, formId)
                                             .build();
                contextBuilder.put(REPORTING_CONTEXT_FORM, formContext);
            }

            PagerData pagerData = layoutState.getPagerData();
            if (pagerData != null) {
                JsonMap pagerContext = JsonMap.newBuilder()
                                              .put(REPORTING_CONTEXT_PAGER_ID, pagerData.getIdentifier())
                                              .put(REPORTING_CONTEXT_PAGER_COUNT, pagerData.getCount())
                                              .put(REPORTING_CONTEXT_PAGER_INDEX, pagerData.getIndex())
                                              .put(REPORTING_CONTEXT_PAGER_COMPLETED, pagerData.isCompleted())
                                              .build();

                contextBuilder.put(REPORTING_CONTEXT_PAGER, pagerContext);
            }
        }

        JsonMap contextData = contextBuilder.build();
        return contextData.isEmpty() ? null : contextData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InAppReportingEvent event = (InAppReportingEvent) o;
        return ObjectsCompat.equals(type, event.type) && ObjectsCompat.equals(scheduleId, event.scheduleId) &&
                ObjectsCompat.equals(source, event.source) && ObjectsCompat.equals(renderedLocale, event.renderedLocale) &&
                ObjectsCompat.equals(campaigns, event.campaigns) && ObjectsCompat.equals(reportingContext, event.reportingContext) &&
                ObjectsCompat.equals(layoutState, event.layoutState) && ObjectsCompat.equals(overrides, event.overrides);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(type, scheduleId, source, renderedLocale, campaigns, reportingContext, layoutState, overrides);
    }

    @NonNull
    private static JsonValue createEventId(@NonNull String scheduleId, @NonNull @InAppMessage.Source String source, @Nullable JsonValue campaigns) {
        switch (source) {
            case InAppMessage.SOURCE_LEGACY_PUSH:
                return JsonValue.wrap(scheduleId);

            case InAppMessage.SOURCE_REMOTE_DATA:
                return JsonMap.newBuilder()
                              .put(MESSAGE_ID, scheduleId)
                              .put(CAMPAIGNS, campaigns)
                              .build()
                              .toJsonValue();

            case InAppMessage.SOURCE_APP_DEFINED:
                return JsonMap.newBuilder()
                              .put(MESSAGE_ID, scheduleId)
                              .build()
                              .toJsonValue();
        }

        return JsonValue.NULL;
    }

    private static class AnalyticsEvent extends Event {

        private final String type;
        private final JsonMap data;

        private AnalyticsEvent(@NonNull String type, @NonNull JsonMap data) {
            this.type = type;
            this.data = data;
        }

        @NonNull
        @Override
        public String getType() {
            return type;
        }

        @NonNull
        @Override
        public JsonMap getEventData() {
            return data;
        }

        @Override
        public String toString() {
            return "AnalyticsEvent{" +
                    "type='" + type + '\'' +
                    ", data=" + data +
                    '}';
        }

    }

}