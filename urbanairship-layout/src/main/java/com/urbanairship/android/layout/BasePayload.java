/* Copyright Airship and Contributors */

package com.urbanairship.android.layout;

import com.urbanairship.android.layout.model.BaseModel;
import com.urbanairship.android.layout.reporting.ReportingContext;
import com.urbanairship.json.JsonException;
import com.urbanairship.json.JsonList;
import com.urbanairship.json.JsonMap;

import androidx.annotation.NonNull;

public class BasePayload {

    private final int version;

    @NonNull
    private final BasePresentation presentation;

    @NonNull
    private final BaseModel view;

    @NonNull
    private final ReportingContext reportingContext;

    public BasePayload(int version, @NonNull BasePresentation presentation, @NonNull BaseModel view, @NonNull ReportingContext reportingContext) {
        this.version = version;
        this.presentation = presentation;
        this.view = view;
        this.reportingContext = reportingContext;
    }


    @NonNull
    public static BasePayload fromJson(@NonNull JsonMap json) throws JsonException {
        int version = json.opt("version").getInt(-1);
        if (version == -1) {
            throw new JsonException("Failed to parse layout payload! Field 'version' is required.");
        }
        JsonMap presentationJson = json.opt("presentation").optMap();
        BasePresentation presentation = BasePresentation.fromJson(presentationJson);
        JsonMap viewJson = json.opt("view").optMap();
        BaseModel view = Thomas.model(viewJson);
        JsonList contextJson = json.opt("context").optList();
        ReportingContext context = ReportingContext.fromJson(contextJson);

        return new BasePayload(version, presentation, view, context);
    }


    public static int versionFromJson(@NonNull JsonMap json) {
        return json.opt("version").getInt(-1);
    }

    public int getVersion() {
        return version;
    }

    @NonNull
    public BasePresentation getPresentation() {
        return presentation;
    }

    @NonNull
    public BaseModel getView() {
        return view;
    }

    @NonNull
    public ReportingContext getReportingContext() {
        return reportingContext;
    }
}
