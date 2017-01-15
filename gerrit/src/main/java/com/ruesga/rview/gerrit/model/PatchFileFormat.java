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
package com.ruesga.rview.gerrit.model;

public enum PatchFileFormat {
    BASE64("application/octet-stream", "diff.base64", "download"),
    ZIP("application/zip", "diff.zip", "zip");

    public final String mMimeType;
    public final String mExtension;
    public final String mOption;
    PatchFileFormat(String mimeType, String ext, String option) {
        mMimeType = mimeType;
        mExtension = ext;
        mOption = option;
    }
}