package scalaz.metrics.http

import org.http4s.implicits._
import org.http4s.server.Router
import scalaz.metrics.PrometheusMetrics
import scalaz.metrics.http.Server._
import scalaz.metrics.http.MetricsService.prometheusMetricsService
import scalaz.zio.interop.catz._
import scalaz.zio.App
import scalaz.zio.interop.catz.taskEffectInstances

import scala.util.Properties.envOrNone

object PrometheusServerTest extends App {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)
  println(s"Starting server on port $port")

  val metrics = new PrometheusMetrics

  def httpApp =
    (metrics: PrometheusMetrics) =>
      Router(
        "/"        -> StaticService.service,
        "/metrics" -> prometheusMetricsService.service(metrics),
        "/measure" -> TestMetricsService.service(metrics)
      ).orNotFound

  override def run(args: List[String]) = builder(httpApp(metrics)).run.map(_ => 0)
}
