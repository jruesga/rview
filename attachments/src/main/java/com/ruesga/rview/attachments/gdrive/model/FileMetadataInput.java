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
package com.ruesga.rview.attachments.gdrive.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

import static com.ruesga.rview.attachments.gdrive.model.Constants.ROOT_FOLDER;

public class FileMetadataInput {
    @SerializedName("appProperties") public Map<String, String> appProperties;
    @SerializedName("contentHints.indexableText") public String contentHintsIndexableText;
    @SerializedName("contentHints.thumbnail.image") public byte[] contentHintsThumbnailImage;
    @SerializedName("contentHints.thumbnail.mimeType") public String contentHintsThumbnailMimeType;
    @SerializedName("createdTime") public String createdTime;
    @SerializedName("description") public String description;
    @SerializedName("folderColorRgb") public String folderColorRgb;
    @NonNull @SerializedName("mimeType") public String mimeType = "application/octet-stream";
    @SerializedName("modifiedTime") public String modifiedTime;
    @NonNull @SerializedName("name") public String name = "";
    @SerializedName("originalFilename") public String originalFilename;
    @NonNull @SerializedName("parents") public String[] parents = new String[]{ROOT_FOLDER};
    @SerializedName("properties") public Map<String, String> properties;
    @SerializedName("starred") public Boolean starred;
    @SerializedName("viewedByMeTime") public String viewedByMeTime;
    @SerializedName("viewersCanCopyContent") public Boolean viewersCanCopyContent;
    @SerializedName("writersCanShare") public Boolean writersCanShare;


}
