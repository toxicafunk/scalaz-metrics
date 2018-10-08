package scalaz.metrics.http

import fs2.StreamApp.ExitCode
import fs2.{ Stream, StreamApp }
import org.http4s.server.blaze._
import scalaz.http.{ MetricsService, StaticService }
import scalaz.metrics.DropwizardMetrics
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Properties.envOrNone

object ServerTest extends StreamApp[Task] {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)
  println(s"Starting server on port $port")

  val metrics = new DropwizardMetrics

  import com.codahale.metrics.jmx.JmxReporter
  val reporter = JmxReporter.forRegistry(metrics.registry).build
  reporter.start()

  override def stream(args: List[String], requestShutdown: Task[Unit]): Stream[Task, ExitCode] =
    BlazeBuilder[Task]
      .bindHttp(port)
      .mountService(StaticService.service, "/")
      .mountService(MetricsService.service(metrics), "/metrics")
      .mountService(TestMetricsService.service(metrics), "/measure")
      .serve
}
