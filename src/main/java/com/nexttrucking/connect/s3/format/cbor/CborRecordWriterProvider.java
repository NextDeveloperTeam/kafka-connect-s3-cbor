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

package com.nexttrucking.connect.s3.format.cbor;

import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.google.common.collect.Streams;
import io.confluent.connect.s3.S3SinkConnectorConfig;
import io.confluent.connect.s3.format.RecordViewSetter;
import io.confluent.connect.s3.format.json.JsonFormat;
import io.confluent.connect.s3.storage.S3OutputStream;
import io.confluent.connect.s3.storage.S3Storage;
import io.confluent.connect.storage.format.RecordWriter;
import io.confluent.connect.storage.format.RecordWriterProvider;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.header.Headers;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import static io.confluent.connect.s3.util.Utils.getAdjustedFilename;

public class CborRecordWriterProvider extends RecordViewSetter
    implements RecordWriterProvider<S3SinkConnectorConfig> {

  private static final Logger log = LoggerFactory.getLogger(CborRecordWriterProvider.class);
  private final S3Storage storage;
  private final CBORMapper mapper;

  public CborRecordWriterProvider(S3Storage storage) {
    this.storage = storage;
    this.mapper = new CBORMapper();
  }

  @Override
  public String getExtension() {
    return ".cbor" + storage.conf().getCompressionType().extension;
  }

  @Override
  public RecordWriter getRecordWriter(final S3SinkConnectorConfig conf, final String filename) {
    try {
      final String adjustedFilename = getAdjustedFilename(recordView, filename, getExtension());
      final S3OutputStream s3out = storage.create(adjustedFilename, true, JsonFormat.class);
      final OutputStream s3outWrapper = s3out.wrapForCompression();
      final CBORGenerator writer = mapper.getFactory()
          .createGenerator(s3outWrapper);

      return new RecordWriter() {
        @Override
        public void write(SinkRecord record) {
          log.trace("Sink record with view {}: {}", recordView, record);

          try {
            writer.writeObject(new CborRecord(
                record.key(),
                record.headers(),
                record.value(),
                record.topic(),
                record.kafkaOffset(),
                record.kafkaPartition(),
                record.timestamp()
            ));
          } catch (IOException e) {
            throw new ConnectException(e);
          }
        }

        @Override
        public void commit() {
          try {
            // Flush is required here, because closing the writer will close the underlying S3
            // output stream before committing any data to S3.
            writer.flush();
            s3out.commit();
            s3outWrapper.close();
          } catch (IOException e) {
            throw new RetriableException(e);
          }
        }

        @Override
        public void close() {
          try {
            writer.close();
          } catch (IOException e) {
            throw new ConnectException(e);
          }
        }
      };
    } catch (IOException e) {
      throw new ConnectException(e);
    }
  }

  class CborRecord {
    private final Object key;
    private final List<HeaderSlim> headers;
    private final Object value;
    private final String topic;
    private final long offset;
    private final int partition;
    private final long timestamp;

    CborRecord(Object key,
               Headers headers,
               Object value,
               String topic,
               long offset,
               int partition,
               long timestamp) {
      this.key = key;
      this.headers = Streams.stream(headers)
          .map(h -> new HeaderSlim(h.key(), h.value()))
          .collect(Collectors.toList());
      this.value = value;
      this.topic = topic;
      this.offset = offset;
      this.partition = partition;
      this.timestamp = timestamp;
    }

    public Object getKey() {
      return key;
    }

    public List<HeaderSlim> getHeaders() {
      return headers;
    }

    public Object getValue() {
      return value;
    }

    public String getTopic() {
      return topic;
    }

    public long getOffset() {
      return offset;
    }

    public int getPartition() {
      return partition;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }

  class HeaderSlim {
    private final String key;
    private final Object value;

    HeaderSlim(String key, Object value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }
  }
}
