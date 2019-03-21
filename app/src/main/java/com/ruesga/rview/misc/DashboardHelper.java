/*
 * Copyright (C) 2017 Jorge Ruesga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ruesga.rview.misc;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.DashboardInfo;
import com.ruesga.rview.gerrit.model.DashboardSectionInfo;
import com.ruesga.rview.preferences.Constants;

import java.util.ArrayList;
import java.util.List;

public class DashboardHelper {
    public static DashboardInfo createDefaultDashboard(Context context) {
        DashboardInfo dashboard = new DashboardInfo();
        dashboard.id = Constants.DASHBOARD_DEFAULT_ID;
        dashboard.title = context.getString(R.string.dashboard_default);
        return dashboard;
    }

    public static DashboardInfo createDashboardFromUri(String dashboardUri) {
        Uri uri = Uri.parse(dashboardUri.replaceFirst("#/dashboard", "dashboard")
                .replaceAll("\\+", "%20"));

        DashboardInfo dashboard = new DashboardInfo();
        dashboard.title = Uri.decode(uri.getQueryParameter("title"));
        String foreach = uri.getQueryParameter("foreach");
        if (foreach != null) {
            foreach = Uri.decode(foreach);
        }

        List<DashboardSectionInfo> sections = new ArrayList<>();
        for (String name : uri.getQueryParameterNames()) {
            DashboardSectionInfo section = new DashboardSectionInfo();
            section.name = Uri.decode(name);
            if ("title".equals(section.name) || "foreach".equals(section.name)) {
                continue;
            }

            section.query = Uri.decode(uri.getQueryParameter(name));
            if (TextUtils.isEmpty(section.name) || TextUtils.isEmpty(section.query)) {
                continue;
            }
            if (foreach != null && !foreach.trim().isEmpty()) {
                //noinspection StringConcatenationInLoop
                section.query += " AND " + foreach;
            }
            sections.add(section);
        }
        dashboard.sections = sections.toArray(new DashboardSectionInfo[0]);

        if (dashboard.title == null || dashboard.sections.length == 0) {
            return null;
        }
        return dashboard;
    }
}
