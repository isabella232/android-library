package com.urbanairship.android.layout.ui;

import android.os.Parcel;

import com.urbanairship.android.layout.BasePayload;
import com.urbanairship.android.layout.event.EventListener;
import com.urbanairship.json.JsonException;
import com.urbanairship.json.JsonValue;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class DisplayArgsLoaderTest extends TestCase {

    private BasePayload payload;

    @Before
    public void setup() throws JsonException {
        String payloadString = "{\n" +
                "    \"layout\": {\n" +
                "        \"version\": 1,\n" +
                "        \"presentation\": {\n" +
                "          \"type\": \"modal\",\n" +
                "          \"default_placement\": {\n" +
                "            \"size\": {\n" +
                "              \"width\": \"100%\",\n" +
                "              \"height\": \"100%\"\n" +
                "            },\n" +
                "            \"position\": { \n" +
                "                \"horizontal\": \"center\",\n" +
                "                \"vertical\": \"center\" \n" +
                "            },\n" +
                "            \"shade_color\": {\n" +
                "              \"default\": { \n" +
                "                  \"type\": \"hex\", \n" +
                "                  \"hex\": \"#000000\", \n" +
                "                  \"alpha\": 0.2 }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"view\": {\n" +
                "            \"type\": \"empty_view\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        JsonValue payload = JsonValue.parseString(payloadString);
        this.payload = BasePayload.fromJson(payload.optMap().opt("layout").optMap());
    }

    @Test
    public void testParcelable() throws DisplayArgsLoader.LoadException {
        EventListener eventListener = mock(EventListener.class);
        DisplayArgs displayArgs = new DisplayArgs(payload, eventListener);
        DisplayArgsLoader loader = DisplayArgsLoader.newLoader(displayArgs);

        // Write
        Parcel parcel = Parcel.obtain();
        loader.writeToParcel(parcel, 0);

        // Reset the parcel so we can read it
        parcel.setDataPosition(0);

        // Read
        DisplayArgsLoader fromParcel = DisplayArgsLoader.CREATOR.createFromParcel(parcel);

        assertEquals(loader.getLayoutArgs().getPayload(), fromParcel.getLayoutArgs().getPayload());
        assertEquals(loader.getLayoutArgs().getEventListener(), fromParcel.getLayoutArgs().getEventListener());
    }

    @Test(expected = DisplayArgsLoader.LoadException.class)
    public void testDismiss() throws DisplayArgsLoader.LoadException {
        EventListener eventListener = mock(EventListener.class);
        DisplayArgs displayArgs = new DisplayArgs(payload, eventListener);
        DisplayArgsLoader loader = DisplayArgsLoader.newLoader(displayArgs);
        loader.dispose();
        loader.getLayoutArgs();
    }

    @Test(expected = DisplayArgsLoader.LoadException.class)
    public void testDismissParcel() throws DisplayArgsLoader.LoadException {
        EventListener eventListener = mock(EventListener.class);
        DisplayArgs displayArgs = new DisplayArgs(payload, eventListener);
        DisplayArgsLoader loader = DisplayArgsLoader.newLoader(displayArgs);

        Parcel parcel = Parcel.obtain();
        loader.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DisplayArgsLoader fromParcel = DisplayArgsLoader.CREATOR.createFromParcel(parcel);

        loader.dispose();
        fromParcel.getLayoutArgs();
    }
}
