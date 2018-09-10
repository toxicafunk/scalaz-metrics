package scalaz.metrics
import org.specs2.ScalaCheck

import scalaz._, Scalaz._
import scalaz.zio.IO

import scala.concurrent.duration._

object DropwizardMetricsSpec extends ScalaCheck {

  val dropwizardMetrics = new DropwizardMetrics

  val f = IO.point(println("Hola"))

  def increaseCounter() = {
    val a = dropwizardMetrics.counter(Label(Array("test", "counter")))

    println(s"a = ${a.repeat(3.seconds).attempt}")
  }

  def main(args: Array[String]): Unit = {
    val a = dropwizardMetrics.counter(Label(Array("test", "counter")))
    a.attempt.map(b => {
      println(b)
      b.fold(_ => 1, _ => 0)
    })
  }
}
