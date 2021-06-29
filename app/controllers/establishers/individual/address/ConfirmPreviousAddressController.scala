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

package controllers.establishers.individual.address

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.ManualAddressController
import forms.address.AddressFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address.PreviousAddressId

import javax.inject.Inject
import models.{Address, Index, AddressConfiguration}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{MessagesApi, Messages, I18nSupport}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport

import scala.concurrent.ExecutionContext

class ConfirmPreviousAddressController @Inject()(override val messagesApi: MessagesApi,
  val userAnswersCacheConnector: UserAnswersCacheConnector,
  val navigator: CompoundNavigator,
  authenticate: AuthAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AddressFormProvider,
  val controllerComponents: MessagesControllerComponents,
  val config: AppConfig,
  val renderer: Renderer
)(implicit ec: ExecutionContext) extends ManualAddressController
  with Retrievals with I18nSupport with NunjucksSupport {

  override protected val pageTitleEntityTypeMessageKey: Option[String] = Some("establisherEntityTypeIndividual")
  override protected val h1MessageKey: String = "previousAddress.title"
  override protected val pageTitleMessageKey: String = "previousAddress.title"

  def form(implicit messages: Messages): Form[Address] = formProvider()

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (EstablisherNameId(index) and SchemeNameId).retrieve.right.map { case establisherName ~ schemeName =>
          get(Some(schemeName), establisherName.fullName, PreviousAddressId(index), AddressConfiguration.PostcodeFirst)
      }
    }

  def onSubmit(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (EstablisherNameId(index) and SchemeNameId).retrieve.right.map { case establisherName ~ schemeName =>
        post(Some(schemeName), establisherName.fullName, PreviousAddressId(index), AddressConfiguration.PostcodeFirst)
      }
    }
}
