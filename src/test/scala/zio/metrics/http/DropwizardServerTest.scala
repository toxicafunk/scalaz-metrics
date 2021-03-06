package zio.metrics.http

import com.codahale.metrics.jmx.JmxReporter
import org.http4s.implicits._
import org.http4s.server.Router
import zio.metrics.DropwizardMetrics
import zio.metrics.http.MetricsService.dropwizardMetricsService
import zio.metrics.http.Server._
import zio.interop.catz._
import zio.interop.catz.taskEffectInstances

import scala.util.Properties.envOrNone
import zio.system.System
import zio.clock.Clock
import zio.console.Console
import zio.random.Random
import zio.blocking.Blocking
import zio.App

object DropwizardServerTest extends App {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)
  println(s"Starting server on port $port")

  val metrics = new DropwizardMetrics

  val reporter: JmxReporter = JmxReporter.forRegistry(metrics.registry).build
  reporter.start()

  val httpApp =
    (metrics: DropwizardMetrics) =>
      Router(
        "/"        -> StaticService.service,
        "/metrics" -> dropwizardMetricsService.service(metrics),
        "/measure" -> TestMetricsService.service(metrics)
      ).orNotFound

  override def run(args: List[String]) =
    builder(httpApp(metrics))
      .provideSome[HttpEnvironment] { rt =>
        new Clock with Console with System with Random with Blocking {
          override val clock: Clock.Service[Any] = rt.clock
          //override val scheduler: Scheduler.Service[Any] = rt.scheduler
          override val console: Console.Service[Any]   = rt.console
          override val random: Random.Service[Any]     = rt.random
          override val system: System.Service[Any]     = rt.system
          override val blocking: Blocking.Service[Any] = rt.blocking
        }
      }
      .run
      .map(_ => 0)
}
