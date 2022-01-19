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

package controllers.establishers.company.director.address

import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import controllers.address.CommonAddressYearsController
import forms.address.AddressYearsFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.address.AddressYearsId
import identifiers.trustees.individual.address.{AddressYearsId => trusteeAddressYearsId}
import models.{CheckMode, Index, Mode, NormalMode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.DataUpdateService
import utils.{Enumerable, UserAnswers}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AddressYearsController @Inject()(override val messagesApi: MessagesApi,
                                       val userAnswersCacheConnector: UserAnswersCacheConnector,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val navigator: CompoundNavigator,
                                       formProvider: AddressYearsFormProvider,
                                       dataUpdateService: DataUpdateService,
                                       val controllerComponents: MessagesControllerComponents,
                                       val renderer: Renderer)
                                      (implicit ec: ExecutionContext)
  extends CommonAddressYearsController
    with Enumerable.Implicits {
  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (DirectorNameId(establisherIndex, directorIndex) and SchemeNameId).retrieve.right.map {
        case directorName ~ schemeName =>
          get(Some(schemeName), directorName.fullName, Messages("messages__director"), form, AddressYearsId(establisherIndex, directorIndex))
      }
    }

  private def form: Form[Boolean] =
    formProvider("individualAddressYears.error.required")

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (DirectorNameId(establisherIndex, directorIndex) and SchemeNameId).retrieve.right.map {
        case directorName ~ schemeName =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                renderer.render(viewTemplate, json(Some(schemeName), directorName.fullName, Messages("messages__director"), formWithErrors)).map(BadRequest(_))
              },
              value =>
                for {
                  updatedAnswers <- Future.fromTry(setUpdatedAnswers(establisherIndex, directorIndex, mode, value, request.userAnswers))
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield {
                  val finalMode = Some(mode).getOrElse(NormalMode)
                  Redirect(navigator.nextPage(AddressYearsId(establisherIndex, directorIndex), updatedAnswers, finalMode))
                }
            )
      }
    }

  private def setUpdatedAnswers(establisherIndex: Index, directorIndex: Index, mode: Mode, value: Boolean, ua: UserAnswers): Try[UserAnswers] = {
    var updatedUserAnswers: Try[UserAnswers] = Try(ua)
    if (mode == CheckMode) {
      val trustee = dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua)
      if (trustee.isDefined)
        updatedUserAnswers = ua.set(trusteeAddressYearsId(trustee.get.index), value)
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.get.set(AddressYearsId(establisherIndex, directorIndex), value)
    finalUpdatedUserAnswers
  }
}
