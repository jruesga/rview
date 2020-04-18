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

import android.os.Build;

import com.ruesga.rview.gerrit.model.DashboardInfo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
// Roboelectric requires Java 9 in Android 29. Stick with Android 28 for now.
@Config(sdk= Build.VERSION_CODES.P, manifest = Config.NONE)
public class DashboardHelperTest {
    @Test
    public void testCreateDashboardFromUri() {
        DashboardInfo dashboard = DashboardHelper.createDashboardFromUri(
                "https://review.lineageos.org/#/dashboard/?" +
                        "title=Dashboard&Own=owner:test1&Pending=project:rview+is:open");
        Assert.assertNotNull(dashboard);
        Assert.assertEquals("Dashboard", dashboard.title);
        Assert.assertEquals(2, dashboard.sections.length);
        Assert.assertEquals("Own", dashboard.sections[0].name);
        Assert.assertEquals("owner:test1", dashboard.sections[0].query);
        Assert.assertEquals("Pending", dashboard.sections[1].name);
        Assert.assertEquals("project:rview is:open", dashboard.sections[1].query);

        dashboard = DashboardHelper.createDashboardFromUri(
                "https://review.lineageos.org/#/dashboard/?" +
                        "title=Dashboard2&Own=owner:test1");
        Assert.assertNotNull(dashboard);
        Assert.assertEquals("Dashboard2", dashboard.title);
        Assert.assertEquals(1, dashboard.sections.length);
        Assert.assertEquals("Own", dashboard.sections[0].name);
        Assert.assertEquals("owner:test1", dashboard.sections[0].query);

        dashboard = DashboardHelper.createDashboardFromUri(
                "https://review.lineageos.org/dashboard/?" +
                        "title=Mine&foreach=owner:self&My+Pending=is:open&My+Merged=is:merged");
        Assert.assertNotNull(dashboard);
        Assert.assertEquals("Mine", dashboard.title);
        Assert.assertEquals(2, dashboard.sections.length);
        Assert.assertEquals("My Pending", dashboard.sections[0].name);
        Assert.assertEquals("is:open AND owner:self", dashboard.sections[0].query);
        Assert.assertEquals("My Merged", dashboard.sections[1].name);
        Assert.assertEquals("is:merged AND owner:self", dashboard.sections[1].query);
    }

}
