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
import models.requests.DataRequest
import models.{Mode, ReferenceValue}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

trait EnterReferenceValueController
  extends FrontendBaseController
    with I18nSupport
    with Retrievals
    with NunjucksSupport {

  protected implicit def executionContext: ExecutionContext

  protected val renderer: Renderer

  protected val userAnswersCacheConnector: UserAnswersCacheConnector

  protected def navigator: CompoundNavigator

  protected def templateName(paragraphText: Seq[String], hintText: Option[String]) : String=
    if (paragraphText.nonEmpty || hintText.nonEmpty) "enterReferenceValueWithHint.njk" else "enterReferenceValue.njk"

  def get(
           pageTitle: String,
           pageHeading: String,
           isPageHeading: Boolean,
           id: TypedIdentifier[ReferenceValue],
           form: Form[ReferenceValue],
           schemeName: String,
           hintText: Option[String] = None,
           paragraphText: Seq[String] = Seq(),
           legendClass: String = "govuk-fieldset__legend--s"
         )(implicit request: DataRequest[AnyContent]): Future[Result] = {

    renderer.render(
      template = templateName(paragraphText, hintText),
      ctx = Json.obj(
        "pageTitle"     -> pageTitle,
        "pageHeading" -> pageHeading,
        "isPageHeading" -> isPageHeading,
        "form"          -> request.userAnswers.get[ReferenceValue](id).fold(form)(form.fill),
        "schemeName"    -> schemeName,
        "legendClass"   -> legendClass,
        "paragraphs"    -> paragraphText
      ) ++ hintText.fold(Json.obj())(text => Json.obj("hintText" -> JsString(text)))
    ).map(Ok(_))
  }

  def post(
            pageTitle: String,
            pageHeading: String,
            isPageHeading: Boolean,
            id: TypedIdentifier[ReferenceValue],
            form: Form[ReferenceValue],
            schemeName: String,
            hintText: Option[String] = None,
            paragraphText: Seq[String] = Seq(),
            legendClass: String = "govuk-fieldset__legend--s",
            mode: Mode
          )(implicit request: DataRequest[AnyContent]): Future[Result] =

    form.bindFromRequest().fold(
      (formWithErrors: Form[_]) =>
        renderer.render(
          template = templateName(paragraphText, hintText),
          ctx = Json.obj(
            "pageTitle"     -> pageTitle,
            "pageHeading" -> pageHeading,
            "isPageHeading" -> isPageHeading,
            "form"          -> formWithErrors,
            "schemeName"    -> schemeName,
            "legendClass"   -> legendClass,
            "paragraphs"    -> paragraphText
          ) ++ hintText.fold(Json.obj())(text => Json.obj("hintText" -> JsString(text)))
        ).map(BadRequest(_)),
      value =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(id, value))
          _              <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
        } yield
          Redirect(navigator.nextPage(id, updatedAnswers, mode))
    )
}
