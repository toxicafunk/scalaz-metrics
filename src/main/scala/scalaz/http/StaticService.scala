package scalaz.http

import java.io.File

import org.http4s._
import org.http4s.dsl.io._
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

object StaticService {
  val service = HttpService[Task] {
    case request @ GET -> Root / name =>
      StaticFile
        .fromFile(new File(s"dist/$name"), Some(request))
        .getOrElse(Response.notFound)
  }
}
