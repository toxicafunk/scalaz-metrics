package scalaz.metrics.http

import cats.data.Kleisli
import com.codahale.metrics.jmx.JmxReporter
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.{Request, Response}
import scalaz.metrics.DropwizardMetrics
import scalaz.metrics.http.Server._
import scalaz.zio.interop.catz._
import scalaz.zio.{App, TaskR}
import scalaz.zio.interop.catz.taskEffectInstances

import scala.util.Properties.envOrNone

object DropwizardServerTest extends App {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)
  println(s"Starting server on port $port")

  val metrics = new DropwizardMetrics

  val reporter: JmxReporter = JmxReporter.forRegistry(metrics.registry).build
  reporter.start()

  def httpApp[A]: HttpApp =
    (metrics: DropwizardMetrics) =>
      Router(
        "/"        -> StaticService.service,
        "/metrics" -> DropwizardMetricsService.service(metrics),
        "/measure" -> TestMetricsService.service(metrics)
      ).orNotFound

  override def run(args: List[String]) = builder(httpApp, metrics).run.map(_ => 0)


  /*BlazeServerBuilder[HttpTask]
      .bindHttp(port)
      .withHttpApp(httpApp(metrics))
      .serve
      .compile
      .drain
      .run
      .map(_ => 0)*/
}
