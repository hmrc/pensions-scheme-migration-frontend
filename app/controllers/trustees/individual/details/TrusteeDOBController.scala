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

package controllers.trustees.individual.details

import connectors.cache.UserAnswersCacheConnector
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import controllers.dateOfBirth.DateOfBirthController
import forms.DOBFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.details.DirectorDOBId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.details.TrusteeDOBId
import models.{CheckMode, Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.DataUpdateService
import uk.gov.hmrc.viewmodels.DateInput
import utils.UserAnswers

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TrusteeDOBController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          val navigator: CompoundNavigator,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: DOBFormProvider,
                                          dataUpdateService: DataUpdateService,
                                          val controllerComponents: MessagesControllerComponents,
                                          val userAnswersCacheConnector: UserAnswersCacheConnector,
                                          val renderer: Renderer
                                        )(implicit val executionContext: ExecutionContext)
  extends DateOfBirthController {

  val form: Form[LocalDate] = formProvider()

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        SchemeNameId.retrieve.map {
          schemeName =>
            get(
              dobId        = TrusteeDOBId(index),
              personNameId = TrusteeNameId(index),
              schemeName   = schemeName,
              entityType   = Messages("messages__individual")
            )
        }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        retrieve(SchemeNameId) {
          schemeName =>
            form.bindFromRequest().fold(
              formWithErrors => {
                val formWithErrorsDayIdCorrection = formWithErrors.copy(
                  errors = formWithErrors.errors map { e => if (e.key == "date.day") e.copy(key = "date") else e }
                )
                TrusteeNameId(index).retrieve.map {
                  personName =>
                    renderer.render(
                      template = "dob.njk",
                      ctx = Json.obj(
                        "form" -> formWithErrorsDayIdCorrection,
                        "date" -> DateInput.localDate(formWithErrorsDayIdCorrection("date")),
                        "name" -> personName.fullName,
                        "schemeName" -> schemeName,
                        "entityType" -> Messages("messages__individual")
                      )
                    ).map(BadRequest(_))
                }
              },
              value =>
                for {
                  updatedAnswers <- Future.fromTry(setUpdatedAnswers(index, mode, value, request.userAnswers))
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield
                  Redirect(navigator.nextPage(TrusteeDOBId(index), updatedAnswers, mode))
            )
        }
    }

  private def setUpdatedAnswers(index: Index, mode: Mode, value: LocalDate, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          val directors = dataUpdateService.findMatchingDirectors(index)(ua)
          directors.foldLeft[UserAnswers](ua) { (acc, director) =>
            if (director.isDeleted)
              acc
            else
              acc.setOrException(DirectorDOBId(director.mainIndex.get, director.index), value)
          }
        case _ => ua
      }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(TrusteeDOBId(index), value)
    finalUpdatedUserAnswers
  }
}
