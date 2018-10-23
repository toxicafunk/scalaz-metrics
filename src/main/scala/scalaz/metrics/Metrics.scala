package scalaz.metrics

//import javax.management.openmbean.OpenType
import scalaz.{ Semigroup, Show }
import scala.concurrent.duration.TimeUnit

sealed trait Reservoir[+A]

object Reservoir {
  case object Uniform                                 extends Reservoir[Nothing]
  case class Bounded[A](window: Long, unit: TimeUnit) extends Reservoir[A]
  case object ExponentiallyDecaying                   extends Reservoir[Nothing]
}

trait Timer[F[_], A] {
  val a: A
  def apply: F[A]
  def stop(io: F[A]): F[Long]
}

trait HtmlRender[A] {
  def render(a: A): String
}

class Label[A: Show](val labels: Array[A])

object Label {
  def apply[A: Show](arr: Array[A]) = new Label(arr)

  implicit def showInstance[A: Show]: Show[Label[A]] = new Show[Label[A]] {
    override def shows(l: Label[A]): String =
      l.labels.mkString(".")
  }
}

trait Metrics[F[_], Ctx] {

  def counter[L: Show](label: Label[L]): F[Long => F[Unit]]

  def gauge[A: Semigroup, L: Show](label: Label[L])(io: F[() => A]): F[Unit]

  def histogram[A: Numeric, L: Show](
    label: Label[L],
    res: Reservoir[A] = Reservoir.ExponentiallyDecaying
  )(
    implicit
    num: Numeric[A]
  ): F[A => F[Unit]]

  def timer[L: Show](label: Label[L]): F[Timer[F[?], Ctx]]

  def meter[L: Show](label: Label[L]): F[Double => F[Unit]]

  // TODO is this still needed is L is not fixed to the Metrics trait?
  //def contramap[L0, L: Show](f: L0 => L): Metrics[F, Ctx]
}
