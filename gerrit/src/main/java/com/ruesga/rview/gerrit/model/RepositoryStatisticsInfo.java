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

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#repository-statistics-info"
 */
public class RepositoryStatisticsInfo {
    @SerializedName("number_of_loose_objects") public long numberOfLooseObjects;
    @SerializedName("number_of_loose_refs") public long numberOfLooseRefs;
    @SerializedName("number_of_pack_files") public long numberOfPackFiles;
    @SerializedName("number_of_packet_objects") public long numberOfPacketObjects;
    @SerializedName("number_of_packet_refs") public long numberOfPacketRefs;
    @SerializedName("size_of_loose_objects") public long sizeOfLooseObjects;
    @SerializedName("size_of_packed_objects") public long sizeOfPackedObjects;
}

