/*
 * Copyright 2021 NEXT Trucking, Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexttrucking.connect.converters;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.storage.HeaderConverter;

import java.io.IOException;
import java.util.Map;

public class ByteArrayHeaderConverter implements HeaderConverter {
  private static final ConfigDef CONFIG_DEF = new ConfigDef();

  @Override
  public SchemaAndValue toConnectHeader(String topic, String headerKey, byte[] value) {
    return new SchemaAndValue(
        value != null ? Schema.BYTES_SCHEMA : Schema.OPTIONAL_BYTES_SCHEMA,
        value);
  }

  @Override
  public byte[] fromConnectHeader(String topic, String headerKey, Schema schema, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ConfigDef config() {
    return CONFIG_DEF;
  }

  @Override
  public void configure(Map<String, ?> configs) {

  }

  @Override
  public void close() throws IOException {

  }
}
