package ru.substancial.staydrunk

import com.bot4s.telegram.models.Location
import domain.{Point, StayDrunkClient}

import scala.concurrent.{ExecutionContext, Future}

trait Goer[F[_]] {

  def goForDirections(loc: Location): F[String]

  def goForAssertions(loc: Location): F[Unit]

}

trait FGoer extends Goer[Future]

class StubGoer(implicit val ex: ExecutionContext) extends FGoer {

  override def goForAssertions(loc: Location): Future[Unit] = Future {
    Thread.sleep(1000)
  }

  override def goForDirections(loc: Location): Future[String] =
    Future {
      Thread.sleep(1000)
      "https://www.fillmurray.com/640/360"
    }

}

class OkGoer(val client: StayDrunkClient)(implicit val ex: ExecutionContext) extends FGoer {

  def goForDirections(loc: Location): Future[String] = client
    .requestDirection(toPoint(loc))
    .map(_.src)

  def goForAssertions(loc: Location): Future[Unit] = client
    .assertPlace(toPoint(loc))
    .map(_ => Unit)

  def toPoint(loc: Location): Point = Point(loc.latitude.toFloat, loc.longitude.toFloat)


}