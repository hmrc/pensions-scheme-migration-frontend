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

package controllers.benefitsAndInsurance

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.AddressFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.benefitsAndInsurance.{BenefitsInsuranceNameId, InsurerAddressId, InsurerAddressListId}
import models.{Address, AddressConfiguration}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.common.address.CommonManualAddressService
import uk.gov.hmrc.nunjucks.NunjucksSupport

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class InsurerConfirmAddressController @Inject()(
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
  common: CommonManualAddressService
)(implicit ec: ExecutionContext) extends Retrievals with I18nSupport with NunjucksSupport {

  private val pageTitleEntityTypeMessageKey: Option[String] = Some("benefitsInsuranceUnknown")
  private def form: Form[Address] = formProvider()

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (BenefitsInsuranceNameId and SchemeNameId).retrieve.map { case insuranceCompanyName ~ schemeName =>
        common.get(
          Some(schemeName),
          insuranceCompanyName,
          InsurerAddressId,InsurerAddressListId,
          AddressConfiguration.PostcodeFirst,
          form,
          pageTitleEntityTypeMessageKey
        )
      }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (BenefitsInsuranceNameId and SchemeNameId).retrieve.map { case insuranceCompanyName ~ schemeName =>
        common.post(
          Some(schemeName),
          insuranceCompanyName,
          InsurerAddressId,
          AddressConfiguration.PostcodeFirst,
          form = form,
          pageTitleEntityTypeMessageKey = pageTitleEntityTypeMessageKey
        )
      }
    }
}
