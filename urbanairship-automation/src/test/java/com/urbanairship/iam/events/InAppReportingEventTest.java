/* Copyright Airship and Contributors */

package com.urbanairship.iam.events;

import com.urbanairship.analytics.Analytics;
import com.urbanairship.analytics.Event;
import com.urbanairship.android.layout.reporting.FormData;
import com.urbanairship.android.layout.reporting.LayoutData;
import com.urbanairship.android.layout.reporting.PagerData;
import com.urbanairship.iam.ButtonInfo;
import com.urbanairship.iam.EventMatchers;
import com.urbanairship.iam.InAppMessage;
import com.urbanairship.iam.ResolutionInfo;
import com.urbanairship.iam.TextInfo;
import com.urbanairship.iam.custom.CustomDisplayContent;
import com.urbanairship.json.JsonException;
import com.urbanairship.json.JsonMap;
import com.urbanairship.json.JsonValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class InAppReportingEventTest {

    private Analytics mockAnalytics = Mockito.mock(Analytics.class);
    private InAppMessage message;

    @Before
    public void setup() throws JsonException {
        this.message = InAppMessage.newBuilder()
                                   .setName("appDefinedMessage")
                                   .setSource(InAppMessage.SOURCE_REMOTE_DATA)
                                   .setDisplayContent(CustomDisplayContent.fromJson(JsonMap.EMPTY_MAP.toJsonValue()))
                                   .build();
    }

    /**
     * Test button click resolution event.
     */
    @Test
    public void testButtonClickResolutionEvent() {
        ButtonInfo buttonInfo = ButtonInfo.newBuilder()
                                          .setId("button id")
                                          .setLabel(TextInfo.newBuilder()
                                                            .setText("hi")
                                                            .build())
                                          .build();

        InAppReportingEvent.resolution("schedule ID", message, 3500, ResolutionInfo.buttonPressed(buttonInfo))
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("resolution", JsonMap.newBuilder()
                                                                .put("type", "button_click")
                                                                .put("button_id", "button id")
                                                                .put("button_description", "hi")
                                                                .put("display_time", Event.millisecondsToSecondsString(3500))
                                                                .build())
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_RESOLUTION, expectedData)));
    }

    /**
     * Test on click resolution event.
     */
    @Test
    public void testClickedResolutionEvent() {
        ResolutionInfo resolutionInfo = ResolutionInfo.messageClicked();

        InAppReportingEvent.resolution("schedule ID", message, 3500, resolutionInfo)
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("resolution", JsonMap.newBuilder()
                                                                .put("type", "message_click")
                                                                .put("display_time", Event.millisecondsToSecondsString(3500))
                                                                .build())
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_RESOLUTION, expectedData)));
    }

    /**
     * Test user dismissed resolution event.
     */
    @Test
    public void testUserDismissedResolutionEvent() {
        ResolutionInfo resolutionInfo = ResolutionInfo.dismissed();

        InAppReportingEvent.resolution("schedule ID", message, 3500, resolutionInfo)
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("resolution", JsonMap.newBuilder()
                                                                .put("type", "user_dismissed")
                                                                .put("display_time", Event.millisecondsToSecondsString(3500))
                                                                .build())
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_RESOLUTION, expectedData)));
    }

    /**
     * Test timed out resolution event.
     */
    @Test
    public void testTimedOutResolutionEvent() {
        ResolutionInfo resolutionInfo = ResolutionInfo.timedOut();

        InAppReportingEvent.resolution("schedule ID", message, 3500, resolutionInfo)
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("resolution", JsonMap.newBuilder()
                                                                .put("type", "timed_out")
                                                                .put("display_time", Event.millisecondsToSecondsString(3500))
                                                                .build())
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_RESOLUTION, expectedData)));
    }

    /**
     * Test replaced resolution event.
     */
    @Test
    public void testReplacedResolutionEvent() {
        InAppReportingEvent.legacyReplaced("iaa ID", "replacement id")
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", "iaa ID")
                                      .put("resolution", JsonMap.newBuilder()
                                                                .put("type", "replaced")
                                                                .put("replacement_id", "replacement id")
                                                                .build())
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_RESOLUTION, expectedData)));
    }

    /**
     * Test direct open resolution event.
     */
    @Test
    public void testDirectOpenResolutionEvent() {
        InAppReportingEvent.legacyPushOpened("iaa ID")
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", "iaa ID")
                                      .put("resolution", JsonMap.newBuilder()
                                                                .put("type", "direct_open")
                                                                .build())
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_RESOLUTION, expectedData)));
    }

    @Test
    public void testDisplay() {
        InAppReportingEvent.display("schedule ID", message)
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_DISPLAY, expectedData)));
    }

    @Test
    public void testButtonTap() {
        InAppReportingEvent.buttonTap("schedule ID", message, "button id")
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("button_identifier", "button id")
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_BUTTON_TAP, expectedData)));
    }

    @Test
    public void testPageView() {
        PagerData pagerData = new PagerData("pager id", 1, 2, false);
        InAppReportingEvent.pageView("schedule ID", message, pagerData)
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("pager_identifier", "pager id")
                                      .put("page_index", 1)
                                      .put("page_count", 2)
                                      .put("completed", false)
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_PAGE_VIEW, expectedData)));
    }

    @Test
    public void testPagerViewSwipe() {
        PagerData pagerData = new PagerData("pager id", 1, 2, false);

        InAppReportingEvent.pageSwipe("schedule ID", message, pagerData, 1, 0)
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("pager_identifier", "pager id")
                                      .put("to_page_index", 1)
                                      .put("from_page_index", 0)
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_PAGE_SWIPE, expectedData)));
    }

    @Test
    public void testFormDisplay() {
        InAppReportingEvent.formDisplay("schedule ID", message, "form id")
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("form_identifier", "form id")
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_FORM_DISPLAY, expectedData)));
    }

    @Test
    public void testFormResult() {
        Map<String, FormData<?>> children = new HashMap<>();
        children.put("score_id", new FormData.Score(1));
        FormData formData = new FormData.Nps("form_id", "score_id", children);

        InAppReportingEvent.formResult("schedule ID", message, formData)
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("form", formData)
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_FORM_RESULT, expectedData)));
    }

    @Test
    public void testCampaigns() {
        InAppReportingEvent.display("schedule ID", message)
                           .setCampaigns(JsonValue.wrap("campaigns!"))
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .put("campaigns", "campaigns!")
                                                        .build())
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_DISPLAY, expectedData)));
    }

    @Test
    public void testRenderedLocale() {
        message = InAppMessage.newBuilder(message)
                              .setRenderedLocale(JsonMap.newBuilder()
                                                        .put("en", "neat")
                                                        .build()
                                                        .getMap())
                              .build();

        InAppReportingEvent.display("schedule ID", message)
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("locale", JsonMap.newBuilder()
                                                            .put("en", "neat")
                                                            .build())
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_DISPLAY, expectedData)));
    }

    @Test
    public void testContext() {
        PagerData pagerData = new PagerData("pager id", 1, 2, true);
        LayoutData layoutData = new LayoutData("form id", pagerData);
        InAppReportingEvent.display("schedule ID", message)
                           .setLayoutData(layoutData)
                           .setReportingContext(JsonValue.wrap("reporting bits!"))
                           .record(mockAnalytics);

        JsonMap contextData = JsonMap.newBuilder()
                                     .put("reporting_context", "reporting bits!")
                                     .put("pager", JsonMap.newBuilder()
                                                          .put("identifier", "pager id")
                                                          .put("count", 2)
                                                          .put("index", 1)
                                                          .put("completed", true)
                                                          .build())
                                     .put("form", JsonMap.newBuilder()
                                                          .put("identifier", "form id")
                                                          .build())
                                     .build();

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("context", contextData)
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_DISPLAY, expectedData)));
    }

    @Test
    public void testEmptyContextData() {
        InAppReportingEvent.display("schedule ID", message)
                           .setLayoutData(new LayoutData(null, null))
                           .setReportingContext(JsonValue.NULL)
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_DISPLAY, expectedData)));
    }

    @Test
    public void testConversionIds() {
        when(mockAnalytics.getConversionMetadata()).thenReturn("conversion metadata!");
        when(mockAnalytics.getConversionSendId()).thenReturn("conversion send id!");

        InAppReportingEvent.display("schedule ID", message)
                           .setLayoutData(new LayoutData(null, null))
                           .setReportingContext(JsonValue.NULL)
                           .record(mockAnalytics);

        JsonMap expectedData = JsonMap.newBuilder()
                                      .put("source", "urban-airship")
                                      .put("id", JsonMap.newBuilder()
                                                        .put("message_id", "schedule ID")
                                                        .build())
                                      .put("conversion_send_id", "conversion send id!")
                                      .put("conversion_metadata", "conversion metadata!")
                                      .build();

        verify(mockAnalytics).addEvent(argThat(EventMatchers.event(InAppReportingEvent.TYPE_DISPLAY, expectedData)));
    }

}