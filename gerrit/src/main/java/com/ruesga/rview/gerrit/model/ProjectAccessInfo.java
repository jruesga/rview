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

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-access.html#project-access-info"
 */
public class ProjectAccessInfo {
    @SerializedName("revision") public String revision;
    @SerializedName("inherits_from") public ProjectInfo inheritsFrom;
    @SerializedName("local") public Map<String, AccessSectionInfo> local;
    @SerializedName("is_owner") public boolean isOwner;
    @SerializedName("owner_of") public String[] ownerOf;
    @SerializedName("can_upload") public boolean canUpload;
    @SerializedName("can_add") public boolean canAdd;
    @SerializedName("config_visible") public boolean configVisible;
}
