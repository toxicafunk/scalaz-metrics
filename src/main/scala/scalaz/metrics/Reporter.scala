package scalaz.metrics

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

  def report(metrics: MetricRegistry, filter: Option[String]): Map[String, String] = {
    val metricFilter = makeFilter(filter)
    val m = metrics.getCounters(metricFilter).asScala.map( entry => entry._1 -> entry._2.getCount.toString )
    m ++ metrics.getGauges(metricFilter).asScala.map( entry => entry._1 -> entry._2.getValue.toString)
    m ++ metrics.getTimers(metricFilter).asScala.map( entry => entry._1 -> entry._2.getCount.toString)
    m ++ metrics.getHistograms(metricFilter).asScala.map( entry => entry._1 -> entry._2.getCount.toString)
    m ++ metrics.getMeters(metricFilter).asScala.map( entry => entry._1 -> entry._2.getCount.toString)
    // TODO: add means, percentiles, etc
    m.toMap
  }

}
