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

package controllers.establishers.partnership.partner.address

import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import controllers.address.CommonAddressYearsController
import forms.address.AddressYearsFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.address.AddressYearsId
import models.{Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddressYearsController @Inject()(override val messagesApi: MessagesApi,
                                       val userAnswersCacheConnector: UserAnswersCacheConnector,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val navigator: CompoundNavigator,
                                       formProvider: AddressYearsFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       val renderer: Renderer)
                                      (implicit ec: ExecutionContext)
  extends CommonAddressYearsController
    with Enumerable.Implicits
{
  private def form: Form[Boolean] =
    formProvider("individualAddressYears.partnership.partner.error.required")

  def onPageLoad(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (PartnerNameId(establisherIndex, partnerIndex) and SchemeNameId).retrieve.map {
        case partnerName ~ schemeName =>
          get(Some(schemeName), partnerName.fullName, Messages("messages__partner"), form, AddressYearsId(establisherIndex, partnerIndex))
      }
    }

  def onSubmit(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (PartnerNameId(establisherIndex, partnerIndex) and SchemeNameId).retrieve.map {
        case partnerName ~ schemeName =>
          post(Some(schemeName), partnerName.fullName, Messages("messages__partner"), form, AddressYearsId(establisherIndex, partnerIndex), Some(mode))
      }
    }
}

