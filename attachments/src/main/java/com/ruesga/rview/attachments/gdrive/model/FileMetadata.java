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

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class FileMetadata {
    @SerializedName("kind") public String kind;
    @SerializedName("id") public String id;
    @SerializedName("name") public String name;
    @SerializedName("mimeType") public String mimeType;
    @SerializedName("description") public String description;
    @SerializedName("starred") public boolean starred;
    @SerializedName("trashed") public boolean trashed;
    @SerializedName("explicitlyTrashed") public boolean explicitlyTrashed;
    @SerializedName("trashingUser") public UserMetadata trashingUser;
    @SerializedName("trashedTime") public String trashedTime;
    @SerializedName("parents") public String[] parents;
    @SerializedName("properties") public Map<String, String> properties;
    @SerializedName("appProperties") public Map<String, String> appProperties;
    @SerializedName("spaces") public String[] spaces;
    @SerializedName("version") public Long version;
    @SerializedName("webContentLink") public String webContentLink;
    @SerializedName("webViewLink") public String webViewLink;
    @SerializedName("iconLink") public String iconLink;
    @SerializedName("hasThumbnail") public boolean hasThumbnail;
    @SerializedName("thumbnailLink") public String thumbnailLink;
    @SerializedName("thumbnailVersion") public Long thumbnailVersion;
    @SerializedName("viewedByMe") public boolean viewedByMe;
    @SerializedName("viewedByMeTime") public String viewedByMeTime;
    @SerializedName("createdTime") public String createdTime;
    @SerializedName("modifiedTime") public String modifiedTime;
    @SerializedName("modifiedByMeTime") public String modifiedByMeTime;
    @SerializedName("modifiedByMe") public boolean modifiedByMe;
    @SerializedName("sharedWithMeTime") public String sharedWithMeTime;
    @SerializedName("sharingUser") public UserMetadata sharingUser;
    @SerializedName("owners") public UserMetadata[] owners;
    @SerializedName("teamDriveId") public String teamDriveId;
    @SerializedName("lastModifyingUser") public UserMetadata lastModifyingUser;
    @SerializedName("shared") public boolean shared;
    @SerializedName("ownedByMe") public boolean ownedByMe;
    @SerializedName("capabilities") public CapabilitiesMetadata capabilities;
    @SerializedName("viewersCanCopyContent") public boolean viewersCanCopyContent;
    @SerializedName("writersCanShare") public boolean writersCanShare;
    @SerializedName("permissions") public PermissionMetadata[] permissions;
    @SerializedName("hasAugmentedPermissions") public boolean hasAugmentedPermissions;
    @SerializedName("folderColorRgb") public String folderColorRgb;
    @SerializedName("originalFilename") public String originalFilename;
    @SerializedName("fullFileExtension") public String fullFileExtension;
    @SerializedName("fileExtension") public String fileExtension;
    @SerializedName("md5Checksum") public String md5Checksum;
    @SerializedName("size") public long size = -1;
    @SerializedName("quotaBytesUsed") public long quotaBytesUsed = -1;
    @SerializedName("headRevisionId") public String headRevisionId;
    @SerializedName("contentHints") public ContentHintsMetadata contentHints;
    @SerializedName("imageMediaMetadata") public ImageMediaMetadata imageMediaMetadata;
    @SerializedName("videoMediaMetadata") public VideoMediaMetadata videoMediaMetadata;
    @SerializedName("isAppAuthorized") public boolean isAppAuthorized;
}
