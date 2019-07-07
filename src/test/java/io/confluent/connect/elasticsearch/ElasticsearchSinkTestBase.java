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

package io.confluent.connect.elasticsearch;

import static io.confluent.connect.elasticsearch.DataConverter.BehaviorOnNullValues;
import static org.junit.Assert.assertEquals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.confluent.connect.elasticsearch.jest.JestElasticsearchClient;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.After;
import org.junit.Before;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class ElasticsearchSinkTestBase {

  protected static final String TYPE = "kafka-connect";

  protected static final String TOPIC = "topic";
  protected static final int PARTITION = 12;
  protected static final int PARTITION2 = 13;
  protected static final int PARTITION3 = 14;
  protected static final TopicPartition TOPIC_PARTITION = new TopicPartition(TOPIC, PARTITION);
  protected static final TopicPartition TOPIC_PARTITION2 = new TopicPartition(TOPIC, PARTITION2);
  protected static final TopicPartition TOPIC_PARTITION3 = new TopicPartition(TOPIC, PARTITION3);

  protected ElasticsearchContainer container;
  protected ElasticsearchClient client;
  private DataConverter converter;

  @Before
  public void setUp() throws Exception {
    container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:6.4.1");
    container.start();

    client = new JestElasticsearchClient("http://"+container.getHttpHostAddress());
    converter = new DataConverter(true, BehaviorOnNullValues.IGNORE);
  }

  @After
  public void tearDown() throws Exception {
    if (client != null) {
      client.close();
    }
    client = null;
    container.stop();
  }


  protected Struct createRecord(Schema schema) {
    Struct struct = new Struct(schema);
    struct.put("user", "Liquan");
    struct.put("message", "trying out Elastic Search.");
    return struct;
  }

  protected Schema createSchema() {
    return SchemaBuilder.struct().name("record")
        .field("user", Schema.STRING_SCHEMA)
        .field("message", Schema.STRING_SCHEMA)
        .build();
  }

  protected Schema createOtherSchema() {
    return SchemaBuilder.struct().name("record")
        .field("user", Schema.INT32_SCHEMA)
        .build();
  }

  protected Struct createOtherRecord(Schema schema) {
    Struct struct = new Struct(schema);
    struct.put("user", 10);
    return struct;
  }

  protected void verifySearchResults(Collection<SinkRecord> records, boolean ignoreKey, boolean ignoreSchema) throws IOException {
    verifySearchResults(records, TOPIC, ignoreKey, ignoreSchema);
  }

  protected void verifySearchResults(Collection<?> records, String index, boolean ignoreKey, boolean ignoreSchema) throws IOException {
    final JsonObject result = client.search("", index, null);

    final JsonArray rawHits = result.getAsJsonObject("hits").getAsJsonArray("hits");

    assertEquals(records.size(), rawHits.size());

    Map<String, String> hits = new HashMap<>();
    for (int i = 0; i < rawHits.size(); ++i) {
      final JsonObject hitData = rawHits.get(i).getAsJsonObject();
      final String id = hitData.get("_id").getAsString();
      final String source = hitData.get("_source").getAsJsonObject().toString();
      hits.put(id, source);
    }

    for (Object record : records) {
      if (record instanceof SinkRecord) {
        IndexableRecord indexableRecord = converter.convertRecord((SinkRecord) record, index, TYPE, ignoreKey, ignoreSchema);
        assertEquals(indexableRecord.payload, hits.get(indexableRecord.key.id));
      } else {
        assertEquals(record, hits.get("key"));
      }
    }
  }

}
