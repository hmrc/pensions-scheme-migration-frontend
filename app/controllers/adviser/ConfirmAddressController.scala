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

package controllers.adviser

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.ManualAddressController
import forms.address.AddressFormProvider
import identifiers.adviser.{AddressId, AdviserNameId}
import identifiers.beforeYouStart.SchemeNameId
import models.{Address, AddressConfiguration}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConfirmAddressController @Inject()(override val messagesApi: MessagesApi,
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

  override protected val pageTitleEntityTypeMessageKey: Option[String] = Some("messages__pension__adviser")

  def form(implicit messages: Messages): Form[Address] = formProvider()

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (AdviserNameId and SchemeNameId).retrieve.right.map { case adviserName ~ schemeName =>
        get(Some(schemeName), adviserName, AddressId, AddressConfiguration.PostcodeFirst)
      }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (AdviserNameId and SchemeNameId).retrieve.right.map { case adviserName ~ schemeName =>
        post(Some(schemeName), adviserName, AddressId, AddressConfiguration.PostcodeFirst)
      }
    }
}
