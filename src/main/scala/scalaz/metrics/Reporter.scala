package scalaz.metrics

import scalaz._
import scalaz.zio.Task

trait Reporter[Ctx, M <: Metrics[Task[?], Ctx], F[_], A] {

  type Filter    = Option[String]
  type MetriczIO = Metrics[Task[?], Ctx]

  def extractCounters: M => Filter => F[A]
  def extractGauges: M => Filter => F[A]
  def extractTimers: M => Filter => F[A]
  def extractHistograms: M => Filter => F[A]
  def extractMeters: M => Filter => F[A]

}

trait ReportPrinter[Ctx, M <: Metrics[Task[?], Ctx]] {
  def report[F[_], A](metrics: M, filter: Option[String])(
    cons: (String, A) => A
  )(implicit M: Monoid[A], L: Foldable[F], R: Reporter[Ctx, M, F, A]): A
}
