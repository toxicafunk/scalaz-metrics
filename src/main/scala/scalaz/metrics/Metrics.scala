package scalaz.metrics

//import javax.management.openmbean.OpenType
import scalaz.{ Order, Semigroup, Show }

sealed trait Resevoir[+A]

object Resevoir {
  case object Uniform                       extends Resevoir[Nothing]
  case class Bounded[A](lower: A, upper: A) extends Resevoir[A]
}

trait Timer[F[_]] {
  def apply[A](io: F[A]): F[A]
}

trait HtmlRender[A] {
  def render(a: A): String
}

class Label[A: Show](val labels: Array[A])

object Label {
  def apply[A: Show](arr: Array[A]) = new Label(arr)

  implicit def showInstance[A: Show] = new Show[Label[A]] {
    override def shows(l: Label[A]): String =
      l.labels.mkString(".")
  }
}

trait Metrics[F[_]] {

  def counter[L: Show](label: Label[L]): F[Long => F[Unit]]

  def gauge[A: Semigroup, L: Show](label: Label[L])(io: F[A]): F[Unit]

  def histogram[A: Order, L: Show](
    label: Label[L],
    res: Resevoir[A] = Resevoir.Uniform
  )(
    implicit
    num: Numeric[A]
  ): F[A => F[Unit]]

  def timer[L: Show](label: Label[L]): F[Timer[F]]

  def meter[L: Show](label: Label[L]): F[Double => F[Unit]]

  def contramap[L0, L: Show](f: L0 => L): Metrics[F] = ???
}

/* object Main {
  // Example usage:
  for {
    requestCount  <- counter(nel("server", "requests", "failed"))
    _             <- requestCount(1)

    _             <- gague("evictions-count")(cache.getEvictionsCount)

    requestLength <- histogram[Double]("request-length")
    _             <- requestLength(requestLength)

    requestTiming <- timer("request-timing")
    _             <- requestTime(doRequest(x, y, z))
  }


  // Syntax extensions
  io.counter(label)
  io.timed(label)
}
 */
