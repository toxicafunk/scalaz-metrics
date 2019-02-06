package scalaz.metrics.http

import cats.data.Kleisli
import com.codahale.metrics.jmx.JmxReporter
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.{ Request, Response }
import scalaz.metrics.DropwizardMetrics
import scalaz.zio.{ App, Clock, IO }
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._
import org.http4s.implicits._

import scala.util.Properties.envOrNone

object DropwizardServerTest extends App {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)
  println(s"Starting server on port $port")

  implicit val clock: Clock = Clock.Live

  val metrics = new DropwizardMetrics

  val reporter: JmxReporter = JmxReporter.forRegistry(metrics.registry).build
  reporter.start()

  def httpApp[A]: DropwizardMetrics => Kleisli[Task, Request[Task], Response[Task]] =
    (metrics: DropwizardMetrics) =>
      Router(
        "/"        -> StaticService.service,
        "/metrics" -> DropwizardMetricsService.service(metrics),
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
