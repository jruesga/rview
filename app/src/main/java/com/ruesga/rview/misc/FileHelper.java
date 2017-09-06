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

import java.io.File;

public class FileHelper {

    public static File getMostRecentFile(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }

        long lastModified = 0L;
        File mostRecent = null;
        for (File file : directory.listFiles()) {
            long fileLastModified = file.lastModified();
            if (lastModified < fileLastModified) {
                mostRecent = file;
            }
        }
        return mostRecent;
    }
}
