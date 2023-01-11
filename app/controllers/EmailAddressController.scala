/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.cache.UserAnswersCacheConnector
import identifiers.TypedIdentifier
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

trait EmailAddressController
  extends FrontendBaseController
    with I18nSupport
    with Retrievals
    with NunjucksSupport {
  protected implicit def executionContext: ExecutionContext

  protected val renderer: Renderer

  protected val userAnswersCacheConnector: UserAnswersCacheConnector

  protected def navigator: CompoundNavigator

  def get(
           entityName: String,
           entityType: String,
           id: TypedIdentifier[String],
           form: Form[String],
           schemeName: String,
           paragraphText: Seq[String] = Seq()
         )(implicit request: DataRequest[AnyContent]): Future[Result] = {

    renderer.render(
      template = "email.njk",
      ctx = Json.obj(
        "entityName" -> entityName,
        "entityType" -> entityType,
        "form" -> request.userAnswers.get[String](id).fold(form)(form.fill),
        "schemeName" -> schemeName,
        "paragraph" -> paragraphText
      )
    ).map(Ok(_))
  }

  def post(
            entityName: String,
            entityType: String,
            id: TypedIdentifier[String],
            form: Form[String],
            schemeName: String,
            paragraphText: Seq[String] = Seq(),
            mode: Mode
          )(implicit request: DataRequest[AnyContent]): Future[Result] =

    form.bindFromRequest().fold(
      (formWithErrors: Form[_]) =>
        renderer.render(
          template = "email.njk",
          ctx = Json.obj(
            "entityName" -> entityName,
            "entityType" -> entityType,
            "form" -> formWithErrors,
            "schemeName" -> schemeName,
            "paragraph" -> paragraphText
          )
        ).map(BadRequest(_)),
      value =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(id, value))
          _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
        } yield
          Redirect(navigator.nextPage(id, updatedAnswers, mode))
    )
}
