/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package parquet.scrooge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.thrift.protocol.TBinaryProtocol.Factory;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;

import parquet.hadoop.thrift.TestCorruptRecordsBase;
import parquet.hadoop.thrift.ThriftReadSupport;
import parquet.scrooge.test.StructWithUnionV2;
import parquet.scrooge.test.StructWithUnionV2$;

import static org.junit.Assert.assertEquals;

public class TestCorruptRecordsScrooge extends TestCorruptRecordsBase {

  @Override
  public void setupJob(Job job, Path path) throws Exception {
    job.setInputFormatClass(ParquetScroogeInputFormat.class);
    ParquetScroogeInputFormat.setInputPaths(job, path);
    ParquetScroogeInputFormat.setThriftClass(job.getConfiguration(), StructWithUnionV2.class);


    ThriftReadSupport.setRecordConverterClass(job.getConfiguration(), ScroogeRecordConverter.class);

    job.setMapperClass(ReadMapper.class);
    job.setNumReduceTasks(0);
    job.setOutputFormatClass(NullOutputFormat.class);
  }

  @Override
  protected void assertEqualsExcepted(List<parquet.thrift.test.compat.StructWithUnionV2> expected, List<Object> found) throws Exception {
    List<StructWithUnionV2> scroogeExpected = new ArrayList<StructWithUnionV2>();
    for (parquet.thrift.test.compat.StructWithUnionV2 tbase : expected) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      TProtocol out = new Factory().getProtocol(new TIOStreamTransport(baos));
      tbase.write(out);
      TProtocol in = new Factory().getProtocol(new TIOStreamTransport(new ByteArrayInputStream(baos.toByteArray())));
      scroogeExpected.add(StructWithUnionV2$.MODULE$.decode(in));
    }
    assertEquals(scroogeExpected, found);
   }
}
