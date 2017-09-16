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

import com.ruesga.rview.TestUtils;
import com.ruesga.rview.attachments.Attachment;
import com.ruesga.rview.gerrit.model.ChangeMessageInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PowerMockRunner.class)
@PrepareForTest({android.util.Log.class, android.text.TextUtils.class})
public class StringHelperTest {

    @Before
    public void setUp() throws Exception {
        TestUtils.mockCommonAndroidClasses();
    }

    @Test
    public void testExtractAllAttachments() throws IOException {
        ChangeMessageInfo message = TestUtils.loadJson(
                ChangeMessageInfo.class, "/com/ruesga/rview/misc/attachments1.txt");
        List<Attachment> attachments = StringHelper.extractAllAttachments(message);

        assertNotNull(attachments);
        assertEquals(2, attachments.size());
        assertEquals("Yosemite Tree", attachments.get(0).mName);
        assertEquals("image/jpeg", attachments.get(0).mMimeType);
        assertEquals(246994, attachments.get(0).mSize);

        message = TestUtils.loadJson(
                ChangeMessageInfo.class, "/com/ruesga/rview/misc/attachments2.txt");
        attachments = StringHelper.extractAllAttachments(message);
        assertEquals(1, attachments.size());
    }

    @Test
    public void testRemoveExtraLines() throws IOException {
        String testMessage = TestUtils.loadString(
                "/com/ruesga/rview/misc/removeExtraLines.msg.txt");
        String expectedMessage = TestUtils.loadString(
                "/com/ruesga/rview/misc/removeExtraLines.expected.txt");
        assertEquals(expectedMessage, StringHelper.removeLineBreaks(testMessage));
    }
}
