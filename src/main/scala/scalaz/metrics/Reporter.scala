package scalaz.metrics

import com.codahale.metrics.{ MetricFilter, MetricRegistry }

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._
import scalaz.syntax.ToApplyOps

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

  def report(metrics: MetricRegistry, filter: Option[String]): String = {
    val metricFilter = makeFilter(filter)
    val m: Map[String, String] = (
      metrics.getCounters(metricFilter).asScala.map(entry => {val count = entry._2.getCount; println(s"counters: $count"); entry._1 -> count.toString}).toMap
        |@| metrics.getGauges(metricFilter).asScala.map(entry => {val gauge = entry._2.getValue; println(s"gauge: $gauge"); entry._1 -> gauge.toString}).toMap
        |@| metrics.getTimers(metricFilter).asScala.map(entry => entry._1     -> entry._2.getCount.toString).toMap
        |@| metrics.getHistograms(metricFilter).asScala.map(entry => entry._1 -> entry._2.getCount.toString).toMap
        |@| metrics.getMeters(metricFilter).asScala.map(entry => entry._1     -> entry._2.getCount.toString).toMap
    )(_ ++: _ ++: _ ++: _ ++: _)
    // TODO: add means, percentiles, etc
    m.foldLeft("")((acc, entry) => acc + entry._1 + "->" + entry._2 + "\n")
  }

}
