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

package handlers

import config.AppConfig
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.http.Status._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{Request, RequestHeader, Result}
import play.api.{Logger, PlayException}
import play.twirl.api.Html
import views.html.templates.ErrorTemplate
import views.html.{BadRequestView, InternalServerErrorView, NotFoundView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject()(
                              val messagesApi: MessagesApi,
                              config: AppConfig,
                              errorTemplate: ErrorTemplate,
                              badRequestView: BadRequestView,
                              internalServerErrorView: InternalServerErrorView,
                              notFoundView: NotFoundView
                            )(implicit val ec: ExecutionContext)
  extends uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
    with I18nSupport {


  def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: RequestHeader): Future[Html] = {
    implicit def requestImplicit: Request[?] = Request(request, "")
    Future.successful(errorTemplate(pageTitle, heading, Some(message)))
  }

  private val logger = Logger(classOf[ErrorHandler])

  override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = {

    implicit def requestImplicit: Request[?] = Request(request, "")

    statusCode match {
      case BAD_REQUEST =>
        Future.successful(BadRequest(badRequestView()))
      case NOT_FOUND =>
        Future.successful(NotFound(notFoundView(config.yourPensionSchemesUrl)))
      case _ =>
        super.onClientError(request, statusCode, message)
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {

    implicit def requestImplicit: Request[?] = Request(request, "")

    logError(request, exception)
    exception match {
      case ApplicationException(result, _) =>
        Future.successful(result)
      case _ =>
        Future.successful(InternalServerError(internalServerErrorView()).withHeaders(CACHE_CONTROL -> "no-cache"))
    }
  }

  private def logError(request: RequestHeader, ex: Throwable): Unit =
    logger.error(
      """
        |
        |! %sInternal server error, for (%s) [%s] ->
        | """.stripMargin.format(ex match {
        case p: PlayException => "@" + p.id + " - "
        case _ => ""
      }, request.method, request.uri),
      ex
    )
}

case class ApplicationException(result: Result, message: String) extends Exception(message)
