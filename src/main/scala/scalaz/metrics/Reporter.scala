package scalaz.metrics

import argonaut.Argonaut._
import argonaut.Json
import com.codahale.metrics.{MetricFilter, MetricRegistry}

import scala.collection.JavaConverters._


object Reporter {

  def makeFilter(filter: Option[String]): MetricFilter = filter match {
    case Some(s) =>
      s.charAt(0) match {
        case '+' => MetricFilter.startsWith(s.substring(1))
        case '-' => MetricFilter.endsWith(s.substring(1))
        case _   => MetricFilter.contains(s)
      }
    case _ => MetricFilter.ALL
  }

  def extractCounters(filter: MetricFilter ): MetricRegistry => List[Json] = (metrics: MetricRegistry) =>
    metrics.getCounters(filter).asScala.map(entry => jSingleObject(entry._1, jNumber(entry._2.getCount))).toList


  def extractGauges(filter: MetricFilter): MetricRegistry => List[Json] = (metrics: MetricRegistry) =>
    metrics.getGauges(filter).asScala.map(entry => jSingleObject(entry._1, jString(entry._2.getValue.toString))).toList

  def report(metrics: MetricRegistry, filter: Option[String]): List[Json] = {
    val metricFilter = makeFilter(filter)
    extractGauges(metricFilter)(metrics) ++ extractCounters(metricFilter)(metrics)

    //val s = metrics.getGauges(metricFilter)
      //.asScala.map(entry => entry._1 -> entry._2.getValue)
    /* val s = metrics.getTimers(metricFilter)
      //.asScala.map(entry => s"${entry._1} -> ${entry._2.getCount}")
    `` ++ metrics.getHistograms(metricFilter).asScala.map(entry => s"${entry._1} -> ${entry._2.getCount}")
      ++ metrics.getMeters(metricFilter).asScala.map(entry => s"${entry._1} -> ${entry._2.getCount}")
      */
    // TODO: add means, percentiles, etc
  }
}
