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

package controllers.trustees.individual.address

import controllers.Retrievals
import controllers.actions._
import forms.address.AddressFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.{address => Director}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address.{PreviousAddressId, PreviousAddressListId}
import models._
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.DataUpdateService
import services.common.address.CommonManualAddressService
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

class ConfirmPreviousAddressController @Inject()(
   val messagesApi: MessagesApi,
   authenticate: AuthAction,
   getData: DataRetrievalAction,
   requireData: DataRequiredAction,
   formProvider: AddressFormProvider,
   dataUpdateService: DataUpdateService,
   common: CommonManualAddressService
)(implicit ec: ExecutionContext) extends Retrievals with I18nSupport {

  private val pageTitleEntityTypeMessageKey: Option[String] = Some("trusteeEntityTypeIndividual")
  private val pageTitleMessageKey: String = "previousAddress.title"

  private def form: Form[Address] = formProvider()

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (TrusteeNameId(index) and SchemeNameId).retrieve.map {
        case trusteeName ~ schemeName =>
          common.get(
            Some(schemeName),
            trusteeName.fullName,
            PreviousAddressId(index),
            PreviousAddressListId(index),
            AddressConfiguration.PostcodeFirst,
            form,
            pageTitleEntityTypeMessageKey,
            pageTitleMessageKey,
            submitUrl = routes.ConfirmPreviousAddressController.onSubmit(index, mode)
          )
      }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (TrusteeNameId(index) and SchemeNameId).retrieve.map {
        case trusteeName ~ schemeName =>
          common.post(
            Some(schemeName),
            trusteeName.fullName,
            PreviousAddressId(index),
            AddressConfiguration.PostcodeFirst,
            Some(mode),
            form,
            pageTitleEntityTypeMessageKey,
            pageTitleMessageKey,
            submitUrl = routes.ConfirmPreviousAddressController.onSubmit(index, mode),
            Some(setUpdatedAnswers(index, _, mode, request.userAnswers))
          )
      }
    }

  private def setUpdatedAnswers(index: Index, value: Address, mode: Mode, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
      case CheckMode =>
        val directors = dataUpdateService.findMatchingDirectors(index)(ua)
        directors.foldLeft[UserAnswers](ua) { (acc, director) =>
          if (director.isDeleted) acc
          else acc.setOrException(Director.PreviousAddressId(director.mainIndex.get, director.index), value)
        }
      case _ => ua
      }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(PreviousAddressId(index), value)
    finalUpdatedUserAnswers
  }
}
