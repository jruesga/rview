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
package com.ruesga.rview;

import android.text.TextUtils;
import android.util.Log;

import com.ruesga.rview.misc.ContinuousIntegrationHelperTest;
import com.ruesga.rview.misc.SerializationManager;

import org.apache.commons.io.IOUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public final class TestUtils {

    public static void mockCommonAndroidClasses() {
        PowerMockito.mockStatic(Log.class);
        PowerMockito.mockStatic(TextUtils.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String methodName = invocationOnMock.getMethod().getName();
                switch (methodName) {
                    case "isEmpty":
                        String text = invocationOnMock.getArgument(0);
                        return text == null || text.length() == 0;
                }
                return null;
            }
        });
    }

    public static File getRootDirectory() {
        File test = new File("").getAbsoluteFile();
        if (test.getName().equals("rview")) {
            return test;
        }
        if (test.getName().equals("app")) {
            return test.getParentFile();
        }
        return test;
    }

    public  static <T> T loadJson(Class<T> returnType, String resource) throws IOException {
        return SerializationManager.getInstance().fromJson(
                new InputStreamReader(
                        ContinuousIntegrationHelperTest.class.getResourceAsStream(
                                resource), "UTF-8"), returnType);
    }

    public  static String loadString(String resource) throws IOException {
        return IOUtils.toString(new InputStreamReader(
                ContinuousIntegrationHelperTest.class.getResourceAsStream(
                        resource), "UTF-8"));
    }
}