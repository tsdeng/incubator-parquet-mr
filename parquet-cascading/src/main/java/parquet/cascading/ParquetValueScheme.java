/**
 * Copyright 2012 Twitter, Inc.
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
package parquet.cascading;

import java.io.IOException;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.RecordReader;

import cascading.flow.FlowProcess;
import cascading.scheme.Scheme;
import cascading.scheme.SinkCall;
import cascading.scheme.SourceCall;
import cascading.tap.Tap;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import parquet.filter2.predicate.FilterPredicate;
import parquet.hadoop.ParquetInputFormat;
import parquet.hadoop.mapred.Container;
import parquet.hadoop.thrift.ThriftReadSupport;

import static parquet.Preconditions.checkNotNull;

/**
 * A Cascading Scheme that returns a simple Tuple with a single value, the "value" object
 * coming out of the underlying InputFormat.
 *
 * This is an abstract class; implementations are expected to set up their Input/Output Formats
 * correctly in the respective Init methods.
 */
public abstract class ParquetValueScheme<T> extends Scheme<JobConf, RecordReader, OutputCollector, Object[], Object[]>{

  public static class Config {
    private final FilterPredicate filterPredicate;
    private final String projectionString;
    public Config(FilterPredicate filterPredicate, String projectionString) {
      this.filterPredicate = filterPredicate;
      this.projectionString = projectionString;
    }

    public static Builder builder(){
      return new Builder();
    }

    public static class Builder {
      private FilterPredicate filterPredicate = null;
      private String projectionString = null;
      public Builder withFilterPredicate(FilterPredicate f) {
        this.filterPredicate = checkNotNull(f, "filterPredicate");
        return this;
      }
      public Builder withProjectionString(String p) {
        this.projectionString = checkNotNull(p, "projectionString");
        return this;
      }
      public Config build() {
        return new Config(filterPredicate, projectionString);
      }
    }
  }

  private static final long serialVersionUID = 157560846420730043L;
  protected final FilterPredicate filterPredicate;
  protected final String projectionString;

  public ParquetValueScheme() {
    this(Config.builder().build());
  }

  public ParquetValueScheme(FilterPredicate filterPredicate) {
    this(Config.builder().withFilterPredicate(filterPredicate).build());
  }

  public ParquetValueScheme(Config config) {
    this.projectionString = config.projectionString;
    this.filterPredicate = config.filterPredicate;
  }

  private void setProjectionPushdown(JobConf jobConf) {
    if (this.projectionString!= null) {
      ThriftReadSupport.setProjectionPushdown(jobConf, this.projectionString);
    }
  }

  private void setPredicatePushdown(JobConf jobConf) {
    if (this.filterPredicate != null) {
      ParquetInputFormat.setFilterPredicate(jobConf, this.filterPredicate);
    }
  }
  @Override
  public void sourceConfInit(FlowProcess<JobConf> jobConfFlowProcess, Tap<JobConf, RecordReader, OutputCollector> jobConfRecordReaderOutputCollectorTap, final JobConf jobConf) {
    setPredicatePushdown(jobConf);
    setProjectionPushdown(jobConf);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean source(FlowProcess<JobConf> fp, SourceCall<Object[], RecordReader> sc)
      throws IOException {
    Container<T> value = (Container<T>) sc.getInput().createValue();
    boolean hasNext = sc.getInput().next(null, value);
    if (!hasNext) { return false; }

    // Skip nulls
    if (value == null) { return true; }

    sc.getIncomingEntry().setTuple(new Tuple(value.get()));
    return true;
  }

  @Override
  public void sink(FlowProcess<JobConf> fp, SinkCall<Object[], OutputCollector> sc)
      throws IOException {
    TupleEntry tuple = sc.getOutgoingEntry();

    if (tuple.size() != 1) {
      throw new RuntimeException("ParquetValueScheme expects tuples with an arity of exactly 1, but found " + tuple.getFields());
    }

    T value = (T) tuple.getObject(0);
    OutputCollector output = sc.getOutput();
    output.collect(null, value);
  }

}
