/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import identifiers.TypedIdentifier
import identifiers.beforeYouStart.SchemeNameId
import models.requests.{DataRequest, OptionalDataRequest}
import play.api.libs.json.Reads
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result, WrappedRequest}

import scala.concurrent.Future
import scala.language.implicitConversions

trait Retrievals {

  private def dataNotFoundRedirect(implicit request: Request[?]) = Redirect(routes.SessionExpiredController.onPageLoad().absoluteURL())
  private[controllers] def retrieve[A](id: TypedIdentifier[A])
                                      (f: A => Future[Result])
                                      (implicit request: DataRequest[AnyContent], r: Reads[A]): Future[Result] = {
    request.userAnswers.get(id).map(f).getOrElse {
      Future.successful(dataNotFoundRedirect)
    }
  }

  private[controllers] def existingSchemeName[A <: WrappedRequest[AnyContent]](implicit request: A): Option[String] =
    request match {
      case optionalDataRequest: OptionalDataRequest[_] => optionalDataRequest.userAnswers.flatMap(_.get(SchemeNameId))
      case dataRequest: DataRequest[_] =>
        dataRequest.userAnswers.get(SchemeNameId)
      case _ => None
    }


  trait Retrieval[A] {
    self =>

    def retrieve(implicit request: DataRequest[AnyContent]): Either[Future[Result], A]

    def and[B](query: Retrieval[B]): Retrieval[A ~ B] =
      new Retrieval[A ~ B] {
        def retrieve(implicit request: DataRequest[AnyContent]): Either[Future[Result], A ~ B] = {
          for {
            a <- self.retrieve
            b <- query.retrieve
          } yield new ~(a, b)
        }
      }
  }



  // scalastyle:off class.name
  case class ~[A, B](a: A, b: B)

  object Retrieval {

    def apply[A](f: DataRequest[AnyContent] => Either[Future[Result], A]): Retrieval[A] =
      new Retrieval[A] {
        def retrieve(implicit request: DataRequest[AnyContent]): Either[Future[Result], A] =
          f(request)
      }
  }


  implicit def fromId[A](id: TypedIdentifier[A])(implicit rds: Reads[A]): Retrieval[A] =
    Retrieval {
      implicit request =>
        request.userAnswers.get(id) match {
          case Some(value) => Right(value)
          case None => Left(Future.successful(dataNotFoundRedirect))
        }
    }

  implicit def merge(f: Either[Future[Result], Future[Result]]): Future[Result] =
    f.merge
}
