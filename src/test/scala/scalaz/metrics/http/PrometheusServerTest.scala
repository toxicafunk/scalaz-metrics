package scalaz.metrics.http

import cats.data.Kleisli
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.{ Request, Response }
import scalaz.metrics.PrometheusMetrics
import scalaz.zio.interop.catz.taskEffectInstances
import scalaz.zio.{ App, Task }
import scalaz.metrics.http.Server._

import scala.util.Properties.envOrNone

object PrometheusServerTest extends App {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)
  println(s"Starting server on port $port")

  val metrics = new PrometheusMetrics

  def httpApp[A]: PrometheusMetrics => Kleisli[Task, Request[Task], Response[Task]] =
    (metrics: PrometheusMetrics) =>
      Router(
        "/"        -> StaticService.service,
        "/metrics" -> PrometheusMetricsService.service(metrics),
        "/measure" -> TestMetricsService.service(metrics)
      ).orNotFound

  override def run(args: List[String]) = builder(httpApp, metrics)
}
