/*
 * Copyright (C) 2016 Jorge Ruesga
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

import com.ruesga.rview.model.Repository;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UriHelperTest {

    @Test
    public void testExtractChangeId() {
        final String URL = "https://chromium-review.googlesource.com/";
        final Repository repository = new Repository("Chromium", URL, false);

        assertEquals("612345", UriHelper.extractChangeId(
                URL + "#/c/612345", repository));
        assertEquals("612345", UriHelper.extractChangeId(
                URL + "c/612345", repository));
        assertEquals("612345:22:chrome:app:chromium_strings.grd", UriHelper.extractChangeId(
                URL + "c/612345/22/chrome/app/chromium_strings.grd", repository));
        assertEquals("612345:5..10:chrome:app:chromium_strings.grd", UriHelper.extractChangeId(
                URL + "c/612345//5..10/chrome/app/chromium_strings.grd", repository));
        assertEquals("612345", UriHelper.extractChangeId(
                URL + "c/chromium/src/+/612345", repository));
        assertEquals("612345", UriHelper.extractChangeId(
                URL + "c/chromium/src/+/612345", repository));
        assertEquals("612345", UriHelper.extractChangeId(
                URL + "c/chromium/src/+/612345?polygerrit=1", repository));
        assertEquals("612345", UriHelper.extractChangeId(
                URL + "?polygerrit=0#/c/chromium/src/+/612345", repository));
        assertEquals("612345", UriHelper.extractChangeId(
                URL + "#/c/chromium/src/+/612345?polygerrit=0", repository));
        assertEquals("612345:22:chrome:app:chromium_strings.grd", UriHelper.extractChangeId(
                URL + "c/chromium/src/+/612345/22/chrome/app/chromium_strings.grd", repository));
        assertEquals("612345:5..10:chrome:app:chromium_strings.grd", UriHelper.extractChangeId(
                URL + "c/chromium/src/+/612345/5..10/chrome/app/chromium_strings.grd", repository));
    }

}
