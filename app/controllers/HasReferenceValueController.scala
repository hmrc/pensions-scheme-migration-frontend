/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.viewmodels.Radios

import scala.concurrent.{ExecutionContext, Future}

trait HasReferenceValueController
  extends FrontendBaseController
    with I18nSupport
    with Retrievals
    with NunjucksSupport {

  protected implicit def executionContext: ExecutionContext

  protected val renderer: Renderer

  protected val userAnswersCacheConnector: UserAnswersCacheConnector

  protected def navigator: CompoundNavigator

  def templateName(paragraphText: Seq[String]) =
    if (paragraphText.nonEmpty) "hasReferenceValueWithHint.njk" else "hasReferenceValue.njk"

  def get(
           pageTitle: String,
           pageHeading: String,
           isPageHeading: Boolean,
           id: TypedIdentifier[Boolean],
           form: Form[Boolean],
           schemeName: String,
           paragraphText: Seq[String] = Seq(),
           legendClass: String = "govuk-fieldset__legend--s"
         )(implicit request: DataRequest[AnyContent]): Future[Result] = {

    val preparedForm: Form[Boolean] =
      request.userAnswers.get[Boolean](id) match {
        case Some(value) => form.fill(value)
        case _ => form
      }

    renderer.render(
      template = templateName(paragraphText),
      ctx = Json.obj(
        "pageTitle" -> pageTitle,
        "pageHeading" -> pageHeading,
        "isPageHeading" -> isPageHeading,
        "form" -> preparedForm,
        "radios" -> Radios.yesNo(preparedForm("value")),
        "schemeName" -> schemeName,
        "legendClass" -> legendClass,
        "paragraphs" -> paragraphText
      )
    ).map(Ok(_))
  }

  def post(
            pageTitle: String,
            pageHeading: String,
            isPageHeading: Boolean,
            id: TypedIdentifier[Boolean],
            form: Form[Boolean],
            schemeName: String,
            paragraphText: Seq[String] = Seq(),
            legendClass: String = "govuk-fieldset__legend--s",
            mode: Mode
          )(implicit request: DataRequest[AnyContent]): Future[Result] =

    form.bindFromRequest().fold(
      (formWithErrors: Form[_]) =>
        renderer.render(
          template = templateName(paragraphText),
          ctx = Json.obj(
            "pageTitle" -> pageTitle,
            "pageHeading" -> pageHeading,
            "isPageHeading" -> isPageHeading,
            "form" -> formWithErrors,
            "radios" -> Radios.yesNo(formWithErrors("value")),
            "schemeName" -> schemeName,
            "legendClass" -> legendClass,
            "paragraphs" -> paragraphText
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
