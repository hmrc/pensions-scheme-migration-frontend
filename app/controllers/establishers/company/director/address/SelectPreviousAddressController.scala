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

package controllers.establishers.company.director.address

import config.AppConfig
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import controllers.address.{AddressPages, AddressListController}
import forms.address.AddressListFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.address.{EnterPreviousPostCodeId, PreviousAddressId, PreviousAddressListId}
import models.{Mode, Index}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.CountryOptions

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelectPreviousAddressController @Inject()(val appConfig: AppConfig,
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

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        getFormToJson(schemeName, establisherIndex, directorIndex, mode).retrieve.right.map(get)
      }
  }

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>

        val addressPages: AddressPages = AddressPages(
          EnterPreviousPostCodeId(establisherIndex, directorIndex),
          PreviousAddressListId(establisherIndex, directorIndex),
          PreviousAddressId(establisherIndex, directorIndex))

      retrieve(SchemeNameId) { schemeName =>
        getFormToJson(schemeName, establisherIndex, directorIndex, mode).retrieve.right.map(post(_, addressPages, Some(mode)))
      }
    }

  def getFormToJson(schemeName:String,
                    establisherIndex: Index,
                    directorIndex: Index,
                    mode: Mode): Retrieval[Form[Int] => JsObject] =
    Retrieval(
      implicit request =>
        EnterPreviousPostCodeId(establisherIndex, directorIndex).retrieve.right.map { addresses =>

          val msg = request2Messages(request)

          val name = request.userAnswers.get(DirectorNameId(establisherIndex, directorIndex)).map(_.fullName).getOrElse(msg("messages__director"))

          form => Json.obj(
            "form" -> form,
            "addresses" -> transformAddressesForTemplate(addresses, countryOptions),
            "entityType" -> msg("messages__director"),
            "entityName" -> name,
            "enterManuallyUrl" -> routes.ConfirmPreviousAddressController.onPageLoad(establisherIndex, directorIndex, mode).url,
            "schemeName" -> schemeName,
            "h1MessageKey" -> "previousAddressList.title"
          )
        }
    )
}
