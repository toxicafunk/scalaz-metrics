package scalaz.http

import org.http4s.server.{ Server => H4Server }
import org.http4s.server.blaze._
import scalaz.metrics.DropwizardMetrics
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

import scala.util.Properties.envOrNone

object Server {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)

  val builder: DropwizardMetrics => Task[H4Server[Task]] = (metrics: DropwizardMetrics) =>
    BlazeBuilder[Task]
      .bindHttp(port)
      .mountService(StaticService.service, "/")
      .mountService(MetricsService.service(metrics), "/metrics")
      .start
}
