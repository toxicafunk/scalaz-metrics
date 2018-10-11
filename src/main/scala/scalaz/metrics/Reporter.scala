package scalaz.metrics

import argonaut._
import Argonaut._
import argonaut.Json
import com.codahale.metrics.{ MetricFilter, MetricRegistry, Snapshot }
import scalaz._

import scala.collection.JavaConverters._

trait Reporter[F[_], A] {
  val extractCounters: MetricFilter => MetricRegistry => F[A]
  val extractGauges: MetricFilter => MetricRegistry => F[A]
  val extractTimers: MetricFilter => MetricRegistry => F[A]
  val extractHistograms: MetricFilter => MetricRegistry => F[A]
  val extractMeters: MetricFilter => MetricRegistry => F[A]
}

/*trait Reporter[F[_], A] {
  def extractCounters(filter: MetricFilter): MetricRegistry => F[A]
  def extractGauges(filter: MetricFilter): MetricRegistry => F[A]
  def extractTimers(filter: MetricFilter): MetricRegistry => F[A]
  def extractHistograms(filter: MetricFilter): MetricRegistry => F[A]
  def extractMeters(filter: MetricFilter): MetricRegistry => F[A]
}*/

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

  implicit val jsonReporter: Reporter[List, Json] = new Reporter[List, Json] {
    override val extractCounters: MetricFilter => MetricRegistry => List[Json] = (filter: MetricFilter) =>
      (metrics: MetricRegistry) =>
        metrics.getCounters(filter).asScala.map(entry => jSingleObject(entry._1, jNumber(entry._2.getCount))).toList

    override val extractGauges: MetricFilter => MetricRegistry => List[Json] = (filter: MetricFilter) =>
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

    override val extractTimers: MetricFilter => MetricRegistry => List[Json] = (filter: MetricFilter) =>
      (metrics: MetricRegistry) =>
        metrics
          .getTimers(filter)
          .asScala
          .map(entry => {
            Json(
              s"${entry._1}_count"          -> jNumber(entry._2.getCount),
              s"${entry._1}_meanRate"       -> jNumber(entry._2.getMeanRate),
              s"${entry._1}_oneMinRate"     -> jNumber(entry._2.getOneMinuteRate),
              s"${entry._1}_fiveMinRate"    -> jNumber(entry._2.getFiveMinuteRate),
              s"${entry._1}_fifteenMinRate" -> jNumber(entry._2.getFifteenMinuteRate)
            ).deepmerge(extractSnapshot(entry._1, entry._2.getSnapshot))
          })
          .toList

    override val extractHistograms: MetricFilter => MetricRegistry => List[Json] = (filter: MetricFilter) =>
      (metrics: MetricRegistry) =>
        metrics
          .getHistograms(filter)
          .asScala
          .map(entry => {
            (s"${entry._1}_count" -> jNumber(entry._2.getCount)) ->:
              extractSnapshot(entry._1, entry._2.getSnapshot)
          })
          .toList

    override val extractMeters: MetricFilter => MetricRegistry => List[Json] = (filter: MetricFilter) =>
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

  //def report[F[_], A](metrics: MetricRegistry, filter: Option[String])(implicit P: PlusEmpty[F], R: Reporter[F, A]): F[A] = {
  def report[F[_], A](metrics: MetricRegistry, filter: Option[String])(
    cons: (String, A) => A
  )(implicit M: Monoid[A], L: Foldable[F], R: Reporter[F, A]): A = {

    import scalaz.syntax.semigroup._
    //type MetricFunction = MetricFilter => MetricRegistry => F[(String,A)]

    val metricFilter = makeFilter(filter)
    val fs = List(
      ("counters", R.extractCounters),
      ("gauges", R.extractGauges),
      ("timers", R.extractTimers),
      ("histograms", R.extractHistograms),
      ("meters", R.extractMeters)
    )

    fs.foldLeft(M.zero)((acc0, f) => {
      val m = f._2(metricFilter)(metrics)
      acc0 |+| L.foldMap(m)(a => cons(f._1, a))
    })
  }
}
