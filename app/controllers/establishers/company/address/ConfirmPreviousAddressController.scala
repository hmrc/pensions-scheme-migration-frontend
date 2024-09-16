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

package controllers.establishers.company.address

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.AddressFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.address.{PreviousAddressId, PreviousAddressListId}
import models.{Address, AddressConfiguration, Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.common.address.CommonManualAddressService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConfirmPreviousAddressController @Inject()(
  override val messagesApi: MessagesApi,
  val userAnswersCacheConnector: UserAnswersCacheConnector,
  val navigator: CompoundNavigator,
  authenticate: AuthAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AddressFormProvider,
  val controllerComponents: MessagesControllerComponents,
  val config: AppConfig,
  val renderer: Renderer,
  val common: CommonManualAddressService
)(implicit ec: ExecutionContext) extends Retrievals with I18nSupport {

  private val pageTitleEntityTypeMessageKey: Option[String] = Some("establisherEntityTypeCompany")
  //private val h1MessageKey: String = "previousAddress.title"
  private val pageTitleMessageKey: String = "previousAddress.title"

  def form: Form[Address] = formProvider()

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (CompanyDetailsId(index) and SchemeNameId).retrieve.map { case companyDetails ~ schemeName =>
        common.get(
          Some(schemeName),
          companyDetails.companyName,
          PreviousAddressId(index),PreviousAddressListId(index),
          AddressConfiguration.PostcodeFirst,
          form,
          pageTitleEntityTypeMessageKey,
          pageTitleMessageKey
        )
      }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (CompanyDetailsId(index) and SchemeNameId).retrieve.map { case companyDetails ~ schemeName =>
        common.post(
          Some(schemeName),
          companyDetails.companyName,
          PreviousAddressId(index),
          AddressConfiguration.PostcodeFirst,Some(mode),
          form,
          pageTitleEntityTypeMessageKey,
          pageTitleMessageKey
        )
      }
    }
}
