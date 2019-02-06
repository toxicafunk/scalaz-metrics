package scalaz.metrics.http

import cats.data.Kleisli
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.{ Request, Response }
import scalaz.metrics.PrometheusMetrics
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._
import scalaz.zio.{ App, Clock, IO }

import scala.util.Properties.envOrNone

object PrometheusServerTest extends App {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)
  println(s"Starting server on port $port")

  implicit val clock: Clock = Clock.Live

  val metrics = new PrometheusMetrics

  def httpApp[A]: PrometheusMetrics => Kleisli[Task, Request[Task], Response[Task]] =
    (metrics: PrometheusMetrics) =>
      Router(
        "/"        -> StaticService.service,
        "/metrics" -> PrometheusMetricsService.service(metrics),
        "/measure" -> TestMetricsService.service(metrics)
      ).orNotFound

  override def run(args: List[String]): IO[Nothing, ExitStatus] =
    BlazeServerBuilder[Task]
      .bindHttp(port)
      .withHttpApp(httpApp(metrics))
      .serve
      .compile
      .drain
      .run
      .map(_ => ExitStatus.ExitNow(0))
}
