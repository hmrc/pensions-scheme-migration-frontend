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

package controllers.adviser

import config.AppConfig
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import controllers.address.{AddressListController, AddressPages}
import controllers.adviser.routes.ConfirmAddressController
import forms.address.AddressListFormProvider
import identifiers.adviser.{AddressId, AddressListId, AdviserNameId, EnterPostCodeId}
import identifiers.beforeYouStart.SchemeNameId
import models.Mode
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.CountryOptions

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelectAddressController @Inject()(val appConfig: AppConfig,
                                        override val messagesApi: MessagesApi,
                                        val userAnswersCacheConnector: UserAnswersCacheConnector,
                                        val addressLookupConnector: AddressLookupConnector,
                                        val navigator: CompoundNavigator,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: AddressListFormProvider,
                                        countryOptions: CountryOptions,
                                        val controllerComponents: MessagesControllerComponents,
                                        val renderer: Renderer)(implicit val ec: ExecutionContext) extends AddressListController with I18nSupport
  with NunjucksSupport with Retrievals {

  override def form: Form[Int] = formProvider("selectAddress.required")

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async { implicit request =>
    retrieve(SchemeNameId) { schemeName =>
      getFormToJson(schemeName, mode).retrieve.map(get)
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      val addressPages: AddressPages = AddressPages(EnterPostCodeId, AddressListId, AddressId)
      retrieve(SchemeNameId) { schemeName =>
        getFormToJson(schemeName,mode).retrieve.map(post(_, addressPages,manualUrlCall = ConfirmAddressController.onPageLoad))
      }
    }

  def getFormToJson(schemeName: String, mode: Mode): Retrieval[Form[Int] => JsObject] =
    Retrieval(
      implicit request =>
        EnterPostCodeId.retrieve.map { addresses =>

          val msg = request2Messages(request)

          val name = request.userAnswers.get(AdviserNameId).getOrElse(msg("messages__pension__adviser"))

          form =>
            Json.obj(
              "form" -> form,
              "addresses" -> transformAddressesForTemplate(addresses, countryOptions),
              "entityType" -> msg("messages__pension__adviser"),
              "entityName" -> name,
              "enterManuallyUrl" -> ConfirmAddressController.onPageLoad.url,
              "schemeName" -> schemeName
            )
        }
    )
}
