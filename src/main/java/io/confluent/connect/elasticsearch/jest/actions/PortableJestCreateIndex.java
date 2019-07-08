/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.connect.elasticsearch.jest.actions;

import io.confluent.connect.elasticsearch.ElasticsearchClient.Version;
import io.searchbox.indices.CreateIndex;

/**
 * Portable Jest action to create indexes, this action will support ES version 7 with type
 * support still enabled.
 */
public class PortableJestCreateIndex extends CreateIndex.Builder {

  private final Version version;

  public PortableJestCreateIndex(String index, Version version) {
    super(index);
    this.version = version;
  }

  @Override
  public CreateIndex build() {
    if (version.equals(Version.ES_V7)) {
      setParameter("include_type_name", true);
    }
    return super.build();
  }
}
