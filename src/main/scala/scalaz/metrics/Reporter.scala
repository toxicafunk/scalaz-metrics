package scalaz.metrics

import argonaut._
import Argonaut._
import argonaut.Json
import com.codahale.metrics.{ MetricFilter, MetricRegistry, Snapshot }
import scalaz._

import scala.collection.JavaConverters._

/*trait Reporter[F[_], A] {
  val extractCounters: MetricFilter => MetricRegistry => F[A]
  val extractGauges: MetricFilter => MetricRegistry => F[A]
  val extractTimers: MetricFilter => MetricRegistry => F[A]
  val extractHistograms: MetricFilter => MetricRegistry => F[A]
  val extractMeters: MetricFilter => MetricRegistry => F[A]
}*/

trait Reporter[F[_], A] {
  def extractCounters(filter: MetricFilter): MetricRegistry => F[A]
  def extractGauges(filter: MetricFilter): MetricRegistry => F[A]
  def extractTimers(filter: MetricFilter): MetricRegistry => F[A]
  def extractHistograms(filter: MetricFilter): MetricRegistry => F[A]
  def extractMeters(filter: MetricFilter): MetricRegistry => F[A]
}

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

  implicit val JsonReporter: Reporter[List, Json] = new Reporter[List, Json] {
    override def extractCounters(filter: MetricFilter): MetricRegistry => List[Json] =
      (metrics: MetricRegistry) =>
        metrics.getCounters(filter).asScala.map(entry => jSingleObject(entry._1, jNumber(entry._2.getCount))).toList

    override def extractGauges(filter: MetricFilter): MetricRegistry => List[Json] =
      (metrics: MetricRegistry) =>
        metrics
          .getGauges(filter)
          .asScala
          .map(entry => jSingleObject(entry._1, jString(entry._2.getValue.toString)))
          .toList

    def extractSnapshot(name: String, snapshot: Snapshot): Json =
      Json(
        s"${name}_max"    -> jNumber(snapshot.getMax),
        s"${name}_min"    -> jNumber(snapshot.getMin),
        s"${name}_mean"   -> jNumber(snapshot.getMean),
        s"${name}_median" -> jNumber(snapshot.getMedian),
        s"${name}_stdDev" -> jNumber(snapshot.getStdDev),
        s"${name}_75th"   -> jNumber(snapshot.get75thPercentile()),
        s"${name}_95th"   -> jNumber(snapshot.get95thPercentile()),
        s"${name}_98th"   -> jNumber(snapshot.get98thPercentile()),
        s"${name}_99th"   -> jNumber(snapshot.get99thPercentile()),
        s"${name}_999th"  -> jNumber(snapshot.get999thPercentile())
      )

    override def extractTimers(filter: MetricFilter): MetricRegistry => List[Json] =
      (metrics: MetricRegistry) =>
        metrics
          .getTimers(filter)
          .asScala
          .map(entry => {
            Json(
              s"${entry._1}_count" -> jNumber(entry._2.getCount),
              s"${entry._1}_meanRate" -> jNumber(entry._2.getMeanRate),
              s"${entry._1}_oneMinRate" -> jNumber(entry._2.getOneMinuteRate),
              s"${entry._1}_fiveMinRate" -> jNumber(entry._2.getFiveMinuteRate),
              s"${entry._1}_fifteenMinRate" -> jNumber(entry._2.getFifteenMinuteRate)
            ).->: (("snapshot", extractSnapshot(entry._1, entry._2.getSnapshot)))
          })
          .toList

    override def extractHistograms(filter: MetricFilter): MetricRegistry => List[Json] =
      (metrics: MetricRegistry) =>
        metrics
          .getHistograms(filter)
          .asScala
          .map(entry => {
            (s"${entry._1}_count" -> jNumber(entry._2.getCount)) ->:
              extractSnapshot(entry._1, entry._2.getSnapshot)
          })
          .toList

    override def extractMeters(filter: MetricFilter): MetricRegistry => List[Json] =
      (metrics: MetricRegistry) =>
        metrics
          .getMeters(filter)
          .asScala
          .map(entry => {
            Json(
              s"${entry._1}_count"          -> jNumber(entry._2.getCount),
              s"${entry._1}_meanRate"       -> jNumber(entry._2.getMeanRate),
              s"${entry._1}_oneMinRate"     -> jNumber(entry._2.getOneMinuteRate),
              s"${entry._1}_fiveMinRate"    -> jNumber(entry._2.getFiveMinuteRate),
              s"${entry._1}_fifteenMinRate" -> jNumber(entry._2.getFifteenMinuteRate)
            )
          })
          .toList
  }

  def report[F[_], A](metrics: MetricRegistry, filter: Option[String])(implicit P: PlusEmpty[F],
                                                                       R: Reporter[F, A]): F[A] = {
    import scalaz.syntax.plus._

    type MetricFunction = MetricFilter => MetricRegistry => F[A]

    val metricFilter = makeFilter(filter)
    val fs: List[MetricFunction] =
      List(R.extractCounters, R.extractGauges, R.extractTimers, R.extractHistograms, R.extractMeters)
    fs.foldLeft(P.empty[A])((acc, f) => acc <+> f(metricFilter)(metrics))

    // TODO: add means, percentiles, etc
  }
}
