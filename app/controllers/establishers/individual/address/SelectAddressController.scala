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
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import controllers.address.{AddressPages, AddressListController}
import forms.address.AddressListFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.address.{EnterPostCodeId, AddressListId, AddressId}
import models.{Mode, Index}

import javax.inject.Inject
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.CountryOptions

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

  override def form: Form[Int] = formProvider("insurerSelectAddress.required")

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async { implicit request =>
    retrieve(SchemeNameId) { schemeName =>
      getFormToJson(schemeName, index, mode).retrieve.right.map(get)
    }
  }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
        val addressPages: AddressPages = AddressPages(EnterPostCodeId(index), AddressListId(index), AddressId(index))
      retrieve(SchemeNameId) { schemeName =>
        getFormToJson(schemeName, index, mode).retrieve.right.map(post(_, addressPages))
      }
    }

  def getFormToJson(schemeName:String, index: Index, mode: Mode) : Retrieval[Form[Int] => JsObject] =
    Retrieval(
      implicit request =>
        EnterPostCodeId(index).retrieve.right.map { addresses =>

          val msg = request2Messages(request)

          val name = Some("wibble") //request.userAnswers.get(BenefitsInsuranceNameId)
            .getOrElse(msg("benefitsInsuranceUnknown"))

          form => Json.obj(
            "form" -> form,
            "addresses" -> transformAddressesForTemplate(addresses, countryOptions),
            "entityType" -> msg("benefitsInsuranceUnknown"),
            "entityName" -> name,
            "enterManuallyUrl" -> controllers.establishers.individual.address.routes.ConfirmAddressController.onPageLoad(index, mode).url,
            "schemeName" -> schemeName
          )
        }
    )
}
