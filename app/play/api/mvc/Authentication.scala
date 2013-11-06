package play.api.mvc

import models.{User,UserSession}

import play.api.Logger

import play.api.mvc.Results._
import scala.concurrent.{Future,ExecutionContext}
import ExecutionContext.Implicits.global
import play.api.templates.Html

trait Authentication
extends Monitoring
with CookieManagement {

  /**
   * If request has authenticated user state do action A, otherwise do action B
   */
  def IfLoggedIn[A](
    a: Future[SimpleResult],
    b: Future[SimpleResult] = { Future { deny() } }
  )(implicit request:Request[A]):Future[SimpleResult] =
    userVisit[A] flatMap {
      case Some(user:User) => a
      case _               => b
    }

  /**
   * If request has authenticated user state do action A, otherwise do action B
   */
  def VisitAction(
    a: Option[User] => Future[SimpleResult]
  ) = Action.async {
    implicit request:Request[AnyContent] =>

    visit[AnyContent] flatMap {
      case Some(session:UserSession) => {

        User.getById(session.user) flatMap {
          case Some(user:User) => {

            createCookieFromSession(session) match {
              case Some(cookie:Cookie) => {

                a(Some(user)) map {
                  result => result.discardingCookies(DiscardingCookie(userCookieKey)).withCookies(cookie)
                }
              }
              case _ => a(Some(user))
            }
          }
          case _ => a(None)
        }
      }
      case _ => a(None)
    }
  }

  /**
   * If request has authenticated user state do action A, otherwise do action B
   */
  def PublicUserAction(
    a: Request[AnyContent] => Option[User] => Future[SimpleResult]
  ) = Action.async {
    implicit request:Request[AnyContent] =>
    userVisit[AnyContent] flatMap {
      case Some(user:User) => a(request)(Some(user))
      case _               => a(request)(None)
    }
  }

  /**
   * If request has authenticated user state do action A, otherwise do action B
   */
  def OnlyPublicAction(
    a: Request[AnyContent] => Future[SimpleResult],
    b: Future[SimpleResult] = { Future { Unauthorized(views.html.error.denied()) } }
  ) = Action.async {
    implicit request:Request[AnyContent] =>
    userVisit[AnyContent] flatMap {
      case None => a(request)
      case _    => b
    }
  }

  /**
   * If request has authenticated user state do action A, otherwise do action B
   */
  def UserAction(
    a: User => Future[SimpleResult],
    b: Future[SimpleResult] = { Future { Unauthorized(views.html.error.denied()) } }
  ) = Action.async {
    implicit request:Request[AnyContent] =>


    visit[AnyContent] flatMap {
      case Some(session:UserSession) => {

        User.getById(session.user) flatMap {
          case Some(user:User) => {

            createCookieFromSession(session) match {
              case Some(cookie:Cookie) => {

                a(user) map {
                  result => result.discardingCookies(DiscardingCookie(userCookieKey)).withCookies(cookie)
                }
              }
              case _ => a(user)
            }
          }
          case _ => b
        }
      }
      case _ => b
    }
  }

  /**
   * If request has authenticated user state do action A, otherwise do action B
   */
  def IfLoggedInPage[A](
    a: Future[SimpleResult],
    b: Future[SimpleResult] = { Future { Unauthorized(views.html.error.denied()) } }
  )(implicit request:Request[A]):Future[SimpleResult] =
    userVisit[A] flatMap {
      case Some(user:User) => a
      case _               => b
    }
}
