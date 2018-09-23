package scalaz.http

import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import org.http4s.server.blaze._
import scalaz.metrics.DropwizardMetrics
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Properties.envOrNone

object Server extends StreamApp[Task] {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)
  println(s"Starting server on port $port")

  val dropwizardMetrics = new DropwizardMetrics

  override def stream(args: List[String], requestShutdown: Task[Unit]): Stream[Task, ExitCode] = {
    BlazeBuilder[Task]
    BlazeBuilder[Task]
      .bindHttp(port)
      .mountService(StaticService.service, "/")
      .mountService(MetricsService.service(dropwizardMetrics.registry), "/metrics")
      .serve
  }
}
