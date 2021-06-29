/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.dateOfBirth

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import identifiers.TypedIdentifier
import models.{Mode, PersonName}
import models.requests.DataRequest
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DateInput

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

trait DateOfBirthController
  extends FrontendBaseController
    with I18nSupport
    with Retrievals
    with NunjucksSupport {

  protected implicit def executionContext: ExecutionContext

  protected val form: Form[LocalDate]

  protected val renderer: Renderer

  protected val userAnswersCacheConnector: UserAnswersCacheConnector

  protected def navigator: CompoundNavigator

  protected def get(
                     dobId: TypedIdentifier[LocalDate],
                     personNameId: TypedIdentifier[PersonName],
                     schemeName: String
                   )(implicit request: DataRequest[AnyContent]): Future[Result] = {

    val preparedForm: Form[LocalDate] =
      request.userAnswers.get[LocalDate](dobId) match {
        case Some(value) => form.fill(value)
        case _           => form
      }

    personNameId.retrieve.right.map {
      personName =>
        renderer.render(
          template = "dob.njk",
          ctx = Json.obj(
            "form"       -> preparedForm,
            "date"       -> DateInput.localDate(preparedForm("date")),
            "name"       -> personName.fullName,
            "schemeName" -> schemeName
          )
        ).map(Ok(_))
    }
  }

  protected def post(
                      dobId: TypedIdentifier[LocalDate],
                      personNameId: TypedIdentifier[PersonName],
                      schemeName: String,
                      mode: Mode
                    )(implicit request: DataRequest[AnyContent]): Future[Result] =

    form.bindFromRequest().fold(
      formWithErrors => {
        personNameId.retrieve.right.map {
          personName =>
            renderer.render(
              template = "dob.njk",
              ctx = Json.obj(
                "form"       -> formWithErrors,
                "date"       -> DateInput.localDate(formWithErrors("date")),
                "name"       -> personName.fullName,
                "schemeName" -> schemeName
              )
            ).map(BadRequest(_))
        }},
      value =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(dobId, value))
          _              <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
        } yield
          Redirect(navigator.nextPage(dobId, updatedAnswers, mode))
    )
}
