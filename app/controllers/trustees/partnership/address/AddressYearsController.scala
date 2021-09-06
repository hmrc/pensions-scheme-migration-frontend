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

package controllers.trustees.partnership.address

import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import controllers.address.CommonAddressYearsController
import forms.address.AddressYearsFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.partnership.PartnershipDetailsId
import identifiers.trustees.partnership.address.AddressYearsId
import models.Index
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
                                       val renderer: Renderer)(implicit ec: ExecutionContext)
  extends CommonAddressYearsController
    with Enumerable.Implicits {

  private def form: Form[Boolean] =
    formProvider("partnershipAddressYears.error.required")

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (PartnershipDetailsId(index) and SchemeNameId).retrieve.right.map {
        case partnershipDetails ~ schemeName =>
          get(Some(schemeName), partnershipDetails.partnershipName, Messages("messages__partnership"), form, AddressYearsId(index))
      }
    }

  def onSubmit(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (PartnershipDetailsId(index) and SchemeNameId).retrieve.right.map {
        case partnershipDetails ~ schemeName =>
          post(Some(schemeName), partnershipDetails.partnershipName, Messages("messages__partnership"), form, AddressYearsId(index))
      }
    }
}
