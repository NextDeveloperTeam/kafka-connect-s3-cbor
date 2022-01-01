# Kafka Connect S3 CBOR

This library adds support to the Kafka Connect S3 Sink Connector to write
data in CBOR format.  By doing so, the exact original bytes of the source message
is persisted to the output file, thereby enabling 1:1 backups and restoring of original message:

- Keys
- Headers
- Values
- Partition #s
- Timestamps

This library works around documented issues with the `store.kafka.keys` and `store.kafka.headers` options
as these cause failures if a key and/or header is missing (for which we have many topics where keys and headers
are inconsistently set).  Since CBOR is designed for binary data, it also avoids issues when converting
messages to & from byte & string representations and avoids record framing/termination ambiguities
with the built-in ByteArrayConverter that uses newlines for end-of-record marking (see the S3
connector docs for more details).

### Usage

To use this library, dump the jar inside the kafka connect s3 directory of your installation.

Then use this config:

```json
{
  ...
  "format.class": "com.nexttrucking.connect.s3.format.cbor.CborFormat",
  "header.converter": "com.nexttrucking.connect.converters.ByteArrayHeaderConverter",
  ...
}
```

### CBOR format

The emitted CBOR records are key-value pairs of this JSON-style form:

```json
{
  "key": <KEY_BYTES>,
  "headers": [
    {
      "key": "HEADER_KEY_1_STRING",
      "value": <HEADER_VALUE_1_BYTES>
    },
    ...
  ],
  "value": <VALUE_BYTES>,
  "partition": <PARTITION_#_INT>,
  "timestamp": <TIMESTAMP_LONG>,
  "offset": <OFFSET_LONG>
}
```

Approximate Python Sample Code

```python
gzip_stream = s3.get_object(Bucket="<BUCKET>", Key=row[0])['Body'].read()

decoder = CBORDecoder(gzip.GzipFile(fileobj=io.BytesIO(gzip_stream)))

while True:
    try:
        record = decoder.decode()
    except EOFError:
        return

    producer.produce(topic,
                     value=record['value'],
                     key=record['key'],
                     headers=[(x['key'], x['value']) for x in record['headers']],
                     partition=record['partition'],
                     timestamp=record['timestamp'])
```