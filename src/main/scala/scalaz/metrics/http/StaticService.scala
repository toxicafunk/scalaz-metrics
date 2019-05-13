package scalaz.metrics.http

import java.io.File
import java.util.concurrent.Executors

import org.http4s._
import org.http4s.dsl.io._
import scalaz.zio.{DefaultRuntime, TaskR}
import scalaz.zio.clock.Clock
import scalaz.zio.interop.catz._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

object StaticService {

  type HttpTask[A] = TaskR[DefaultRuntime, A]

  val blockingEc: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  implicit val clock: Clock = Clock.Live

  val service: HttpRoutes[HttpTask] = HttpRoutes.of[HttpTask] {
    case request @ GET -> Root / name =>
      StaticFile
        .fromFile(new File(s"dist/$name"), blockingEc, Some(request))
        .getOrElse(Response.notFound)
  }
}
