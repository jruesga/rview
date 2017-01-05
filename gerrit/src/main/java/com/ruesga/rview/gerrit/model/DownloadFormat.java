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

public enum DownloadFormat {
    TGZ("application/x-gzip", "gzip"),
    TAR("application/x-tar", "tar"),
    TBZ2("application/x-tar-bz2", "tar.bz2"),
    TXZ("application/x-xz", "tar.xz");

    public final String mMimeType;
    public final String mExtension;
    DownloadFormat(String mimeType, String ext) {
        mMimeType = mimeType;
        mExtension = ext;
    }
}
