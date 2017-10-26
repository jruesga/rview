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
import android.content.res.Resources;
import android.support.annotation.RawRes;

import com.ruesga.rview.TestUtils;
import com.ruesga.rview.model.Repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class ModelHelperTest {

    private static class MockResources extends Resources {
        public MockResources() {
            //noinspection deprecation
            super(null, null, null);
        }

        @Override
        public InputStream openRawResource(@RawRes int id) throws NotFoundException {
            File res = new File(TestUtils.getRootDirectory(),
                    "app/src/main/res/raw/repositories.json");
            try {
                return new BufferedInputStream(new FileInputStream(res));
            } catch (IOException ex) {
                throw new NotFoundException(res.getAbsolutePath());
            }
        }
    }

    @Mock
    private Context mMockContext;

    @Test
    public void testGetPredefinedRepositories() {
        Mockito.when(mMockContext.getResources()).thenReturn(new MockResources());

        List<Repository> repositories =  ModelHelper.getPredefinedRepositories(mMockContext);
        assertNotNull(repositories);
        int index = 0;
        for (Repository repository : repositories) {
            assertTrue("Repository 'name' cannot be null, at index " + index,
                    repository.mName != null && repository.mName.trim().length() > 0);
            assertNotNull("Repository 'url' cannot be null, at index " + index,
                    repository.mUrl != null && repository.mUrl.trim().length() > 0);
            try {
                new URL(repository.mUrl);
            } catch (Exception ex) {
                fail("Invalid repository 'url', at index " + index);
                ex.printStackTrace();
            }
            assertTrue("Repository 'url' must end with /, at index " + index,
                    repository.mUrl.endsWith("/"));
            if (repository.mCiAccounts != null && repository.mCiAccounts.trim().length() > 0) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Pattern.compile(repository.mCiAccounts);
                } catch (PatternSyntaxException ex) {
                    fail("Invalid repository 'ci_accounts' pattern, at index " + index);
                    ex.printStackTrace();
                }
            }

            index++;
        }
    }

}
