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

public class PermissionMetadata {
    @SerializedName("kind") public String kind;
    @SerializedName("id") public String id;
    @SerializedName("type") public GranteeType type;
    @SerializedName("emailAddress") public String emailAddress;
    @SerializedName("domain") public String domain;
    @SerializedName("role") public RoleType role;
    @SerializedName("allowFileDiscovery") public boolean allowFileDiscovery;
    @SerializedName("displayName") public String displayName;
    @SerializedName("photoLink") public String photoLink;
    @SerializedName("expirationTime") public String expirationTime;
    @SerializedName("teamDrivePermissionDetails") public TeamDrivePermissionDetailMetadata[] teamDrivePermissionDetails;
    @SerializedName("deleted") public boolean deleted;
}
