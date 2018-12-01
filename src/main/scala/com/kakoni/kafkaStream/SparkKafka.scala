package com.kakoni.kafkaStream

import java.util.concurrent.atomic.AtomicInteger
import java.util.{Calendar, UUID}

import com.kakoni.kafkaStream.parser.AccessLogParser
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, KafkaUtils}
import org.apache.spark.streaming.{Seconds, StreamingContext, Time}

/**
  * Hello world!
  *
  */
object SparkKafka{

  def main(args: Array[String]): Unit = {
    println("===================START==================")
    val UID = UUID.randomUUID().toString()
    val conf = new SparkConf()
      .setAppName("SparkKafka")
      .setMaster("local[*]")
      .set("spark.streaming.kafka.maxRatePerPartition", "100")

    val filePath = "/user/data/kafka/apache"
    val spark = SparkSession
      .builder
      .config(conf)
      .getOrCreate()

    val ssc = new StreamingContext(spark.sparkContext, Seconds(10))

    val server = "sandbox-hdp.hortonworks.com:6667"
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> server,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "auto.offset.reset" -> "earliest",
      "group.id" -> "group_id",
      "enable.auto.commit" -> (true: java.lang.Boolean)
    )
    val topics = Array("demo_kafka")

    val stream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](ssc, PreferConsistent,
      Subscribe[String, String](topics, kafkaParams))
    //type Record = ConsumerRecord[String, String]
    val i: AtomicInteger = new AtomicInteger(0)
    stream.foreachRDD {
      (message: RDD[ConsumerRecord[String, String]], batchTime: Time) => {
        val offsetRange = message.asInstanceOf[HasOffsetRanges].offsetRanges
        for (o <- offsetRange) {
          //println(s"\nTopic: ${o.topic} Partition: ${o.partition} FromOffset: ${o.fromOffset} UntilOffset: ${o.untilOffset}")
        }

        // Get the singleton instance of SparkSession
        val spark = SparkSession.builder.config(message.sparkContext.getConf).getOrCreate()
        import spark.implicits._

        val df = message.map(
          msg => AccessLogParser.parser(msg.value())
        ).toDF()

        println("=================== START DF ==================")
        df.show(false)
        df.write.save(filePath+"/" +i.getAndIncrement())
        println(UID + ": " + Calendar.getInstance.getTime + ": Found: " + message.count() + " lines: " + batchTime + " batch")
      }
    }
    ssc.start()
    println("starting")
    ssc.start()
    println("awaiting")
    ssc.awaitTermination()
    println("terminated")
  }
}
