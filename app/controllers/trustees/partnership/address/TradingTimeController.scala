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

package controllers.trustees.partnership.address

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.TradingTimeFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.partnership.PartnershipDetailsId
import identifiers.trustees.partnership.address.TradingTimeId
import models.{Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.common.address.CommonTradingTimeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TradingTimeController @Inject()(
    val messagesApi: MessagesApi,
    val userAnswersCacheConnector: UserAnswersCacheConnector,
    authenticate: AuthAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val navigator: CompoundNavigator,
    formProvider: TradingTimeFormProvider,
    val controllerComponents: MessagesControllerComponents,
    val renderer: Renderer,
    common: CommonTradingTimeService
 )(implicit ec: ExecutionContext) extends Retrievals with Enumerable.Implicits {

  private def form: Form[Boolean] = formProvider("partnershipTradingTime.error.required")

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      implicit val messages: Messages = controllerComponents.messagesApi.preferred(request)

      (PartnershipDetailsId(index) and SchemeNameId).retrieve.map {
        case partnershipDetails ~ schemeName =>
          common.get(
            Some(schemeName),
            partnershipDetails.partnershipName,
            Messages("messages__partnership"),
            form, TradingTimeId(index)
          )
      }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      implicit val messages: Messages = controllerComponents.messagesApi.preferred(request)
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      (PartnershipDetailsId(index) and SchemeNameId).retrieve.map {
        case partnershipDetails ~ schemeName =>
          common.post(
            Some(schemeName),
            partnershipDetails.partnershipName,
            Messages("messages__partnership"),
            form, TradingTimeId(index),
            Some(mode)
          )
      }
    }
}
