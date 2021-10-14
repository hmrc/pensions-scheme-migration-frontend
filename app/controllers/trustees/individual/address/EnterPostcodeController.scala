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

package controllers.trustees.individual.address

import config.AppConfig
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import controllers.address.PostcodeController
import forms.address.PostcodeFormProvider
import helpers.routes.TrusteesIndividualRoutes
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address.EnterPostCodeId
import models.requests.DataRequest
import models.{Mode, Index, NormalMode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EnterPostcodeController @Inject()(val appConfig: AppConfig,
                                               override val messagesApi: MessagesApi,
                                               val userAnswersCacheConnector: UserAnswersCacheConnector,
                                               val addressLookupConnector: AddressLookupConnector,
                                               val navigator: CompoundNavigator,
                                               authenticate: AuthAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               formProvider: PostcodeFormProvider,
                                               val controllerComponents: MessagesControllerComponents,
                                               val renderer: Renderer
                                              )(implicit val ec: ExecutionContext) extends PostcodeController with I18nSupport with NunjucksSupport {

  def form: Form[String] = formProvider("enterPostcode.required", "enterPostcode.invalid")

  def formWithError(messageKey: String): Form[String] = {
    form.withError("value", s"messages__error__postcode_$messageKey")
  }

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        get(getFormToJson(schemeName, index, NormalMode))
      }
    }

  def onSubmit(index: Index): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async{
    implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        post(getFormToJson(schemeName, index, NormalMode), EnterPostCodeId(index), "enterPostcode.invalid")
      }
  }


  def getFormToJson(schemeName:String, index: Index, mode: Mode)(implicit request:DataRequest[AnyContent]): Form[String] => JsObject = {
    form => {
      val msg = request2Messages(request)
      val name = request.userAnswers.get(TrusteeNameId(index)).map(_.fullName).getOrElse(msg("trusteeEntityTypeIndividual"))
      Json.obj(
        "entityType" -> msg("trusteeEntityTypeIndividual"),
        "entityName" -> name,
        "form" -> form,
        "enterManuallyUrl" -> TrusteesIndividualRoutes.confirmAddressRoute(index, mode).url,
        "schemeName" -> schemeName
      )
    }
  }
}
