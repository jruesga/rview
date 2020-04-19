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

public class CapabilitiesMetadata {
    @SerializedName("canAddChildren") public boolean canAddChildren;
    @SerializedName("canChangeViewersCanCopyContent") public boolean canChangeViewersCanCopyContent;
    @SerializedName("canComment") public boolean canComment;
    @SerializedName("canCopy") public boolean canCopy;
    @SerializedName("canDelete") public boolean canDelete;
    @SerializedName("canDownload") public boolean canDownload;
    @SerializedName("canEdit") public boolean canEdit;
    @SerializedName("canListChildren") public boolean canListChildren;
    @SerializedName("canMoveItemIntoTeamDrive") public boolean canMoveItemIntoTeamDrive;
    @SerializedName("canMoveTeamDriveItem") public boolean canMoveTeamDriveItem;
    @SerializedName("canReadRevisions") public boolean canReadRevisions;
    @SerializedName("canReadTeamDrive") public boolean canReadTeamDrive;
    @SerializedName("canRemoveChildren") public boolean canRemoveChildren;
    @SerializedName("canRename") public boolean canRename;
    @SerializedName("canShare") public boolean canShare;
    @SerializedName("canTrash") public boolean canTrash;
    @SerializedName("canUntrash") public boolean canUntrash;
}
