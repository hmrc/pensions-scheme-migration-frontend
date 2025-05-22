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

package controllers.establishers.company.director.address

import controllers.Retrievals
import controllers.actions._
import forms.address.AddressYearsFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.address.AddressYearsId
import identifiers.trustees.individual.address.{AddressYearsId => trusteeAddressYearsId}
import models.{CheckMode, Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.DataUpdateService
import services.common.address.CommonAddressYearsService
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

class AddressYearsController @Inject()(
   val messagesApi: MessagesApi,
   authenticate: AuthAction,
   getData: DataRetrievalAction,
   requireData: DataRequiredAction,
   formProvider: AddressYearsFormProvider,
   dataUpdateService: DataUpdateService,
   common: CommonAddressYearsService
)(implicit ec: ExecutionContext) extends Retrievals with I18nSupport {

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (DirectorNameId(establisherIndex, directorIndex).and(SchemeNameId)).retrieve.map {
        case directorName ~ schemeName =>
          common.get(
            Some(schemeName),
            directorName.fullName,
            Messages("messages__director"),
            form,
            AddressYearsId(establisherIndex, directorIndex),
            submitUrl = routes.AddressYearsController.onSubmit(establisherIndex, directorIndex, mode)
          )
      }
    }

  private def form: Form[Boolean] =
    formProvider("individualAddressYears.error.required")

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (DirectorNameId(establisherIndex, directorIndex).and(SchemeNameId)).retrieve.map {
        case directorName ~ schemeName =>
          common.post(Some(schemeName),
            directorName.fullName,
            Messages("messages__director"),
            form,
            AddressYearsId(establisherIndex, directorIndex),
            Some(mode),
            Some(value => setUpdatedAnswers(establisherIndex, directorIndex, mode, value, request.userAnswers)),
            submitUrl = routes.AddressYearsController.onSubmit(establisherIndex, directorIndex, mode)
          )
      }
    }

  private def setUpdatedAnswers(establisherIndex: Index, directorIndex: Index, mode: Mode, value: Boolean, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
            ua.setOrException(trusteeAddressYearsId(trustee.index), value)
          }.getOrElse(ua)
        case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(AddressYearsId(establisherIndex, directorIndex), value)
    finalUpdatedUserAnswers
  }
}
