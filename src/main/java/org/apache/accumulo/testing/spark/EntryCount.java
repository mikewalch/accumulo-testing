package org.apache.accumulo.testing.spark;

import org.apache.accumulo.core.client.Accumulo;
import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.hadoop.mapred.AccumuloInputFormat;
import org.apache.accumulo.testing.TestEnv;
import org.apache.hadoop.mapred.JobConf;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class EntryCount {

  public static void main(String[] args) throws Exception {

    TestEnv env = new TestEnv(args);

    String table = "spark";
    try (AccumuloClient client = Accumulo.newClient().from(env.getClientProps()).build()) {
      client.tableOperations().create(table);
      try (BatchWriter bw = client.createBatchWriter(table)) {
        for (int i = 0; i < 10000; i++) {
          Mutation m = new Mutation(String.format("%09d", i));
          m.at().family("cf1").qualifier("cq1").put("" + i);
          bw.addMutation(m);
        }
      }
    } catch (TableExistsException e) {
      // ignore
    }

    SparkConf sparkConf = new SparkConf();
    sparkConf.setAppName("EntryCount");
    // sparkConf.set("spark.executor.userClassPathFirst", "true");
    // sparkConf.set("spark.driver.userClassPathFirst", "true");

    JavaSparkContext sc = new JavaSparkContext(sparkConf);

    JobConf job = new JobConf();
    // job.set("mapreduce.job.classloader", "true");
    AccumuloInputFormat.configure().clientProperties(env.getClientProps()).table(table).store(job);
    JavaPairRDD<Key,Value> data = sc.hadoopRDD(job, AccumuloInputFormat.class, Key.class,
        Value.class);
    System.out.println(data.count());
  }
}
