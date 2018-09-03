package scalaz.metrics

//import javax.management.openmbean.OpenType
import scalaz.{Order, Semigroup, Show}

sealed trait Resevoir[+A]
object Resevoir {
  case object Uniform extends Resevoir[Nothing]
  case class Bounded[A](lower: A, upper: A) extends Resevoir[A]
}

trait Timer[F[_]] {
  def apply[A](io: F[A]): F[A]
}

trait HtmlRender[A] {
  def render(a: A): String
}

class Label[A] (val labels: Array[A]){

  def apply(arr: Array[A]) = new Label(arr)

  val toMetricName = labels.foldLeft("")( (b, a) => b ++ a.toString())
}

trait Metrics[C[_], F[_], L] {

  def counter(label: L): F[Long => F[Unit]]

  def gague[A: Semigroup: C](label: L)(io: F[A]): F[Unit]

  def histogram[A: Order: C](
    label: L, 
    res  : Resevoir[A] = Resevoir.Uniform): F[A => F[Unit]]

  def timer(label: L): F[Timer[F]]

  def meter(label: L): F[Double => F[Unit]]

  def contramap[L0](f: L0 => L): Metrics[C, F, L] = ???
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
