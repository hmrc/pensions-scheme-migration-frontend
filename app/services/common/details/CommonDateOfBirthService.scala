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

package services.common.details

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import identifiers.TypedIdentifier
import models.requests.DataRequest
import models.{Mode, PersonName}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Call, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.viewmodels.DateInput
import uk.gov.hmrc.viewmodels.DateInput.ViewModel
import utils.UserAnswers
import views.html.DobView

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class CommonDateOfBirthService @Inject()(val controllerComponents: MessagesControllerComponents,
                                         dobView: DobView,
                                         val userAnswersCacheConnector: UserAnswersCacheConnector,
                                         val navigator: CompoundNavigator,
                                         val messagesApi: MessagesApi)
  extends NunjucksSupport with FrontendHeaderCarrierProvider with I18nSupport with Retrievals {


  private case class TemplateData(form: Form[LocalDate],
                                  date: ViewModel,
                                  name: String,
                                  personNameId: TypedIdentifier[PersonName],
                                  schemeName: String,
                                  entityType: String)

  implicit private def templateDataWrites(implicit request: DataRequest[AnyContent]): OWrites[TemplateData] = Json.writes[TemplateData]

  def get(form: Form[LocalDate],
                    dobId: TypedIdentifier[LocalDate],
                    personNameId: TypedIdentifier[PersonName],
                    schemeName: String,
                    entityType: String,
                    call: Call
                   )(implicit request: DataRequest[AnyContent], ex: ExecutionContext): Future[Result] = {

    val preparedForm: Form[LocalDate] = {
      request.userAnswers.get[LocalDate](dobId) match {
        case Some(value) => form.fill(value)
        case _ => form
      }
    }
    personNameId.retrieve.map {
      personName: PersonName =>
        Future.successful(Ok(dobView(preparedForm, DateInput.localDate(preparedForm("date")), personName.fullName,
             schemeName, entityType, call)))
    }
  }

  def post(form: Form[LocalDate],
                     dobId: TypedIdentifier[LocalDate],
                     personNameId: TypedIdentifier[PersonName],
                     schemeName: String,
                     entityType: String,
                     mode: Mode,
                     optSetUserAnswers: Option[LocalDate => Try[UserAnswers]] = None,
                     call: Call
                    )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] =

    form.bindFromRequest().fold(
      formWithErrors => {
        // This is to get round Nunjucks issue whereby clicking error message relating to day field
        // was not going to the correct input field.
        val formWithErrorsDayIdCorrection = formWithErrors.copy(
          errors = formWithErrors.errors map { e => if (e.key == "date.day") e.copy(key = "date") else e }
        )
        personNameId.retrieve.map {
          personName: PersonName =>

            Future.successful(Ok(dobView(
              formWithErrorsDayIdCorrection, DateInput.localDate(formWithErrorsDayIdCorrection("date")),
                personName.fullName, schemeName, entityType, call)))
        }
      },
      value => {
        def defaultSetUserAnswers = (value: LocalDate) =>
          request.userAnswers.set(dobId, value)

        val setUserAnswers = optSetUserAnswers.getOrElse(defaultSetUserAnswers)

        for {
          updatedAnswers <- Future.fromTry(setUserAnswers(value))
          _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
        } yield
          Redirect(navigator.nextPage(dobId, updatedAnswers, mode))
      }
    )

  private def getTemplateData(form: Form[LocalDate],
                              date: ViewModel,
                              name: String,
                              personNameId: TypedIdentifier[PersonName],
                              schemeName: String,
                              entityType: String): TemplateData = {
    TemplateData(
      form,
      date,
      name,
      personNameId,
      schemeName,
      entityType)
  }
}
