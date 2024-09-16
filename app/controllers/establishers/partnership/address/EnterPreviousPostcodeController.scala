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

package controllers.establishers.partnership.address

import config.AppConfig
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.PostcodeFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.establishers.partnership.address.EnterPreviousPostCodeId
import models.requests.DataRequest
import models.{Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.common.address.CommonPostcodeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EnterPreviousPostcodeController @Inject()(
   val appConfig: AppConfig,
   override val messagesApi: MessagesApi,
   val userAnswersCacheConnector: UserAnswersCacheConnector,
   val addressLookupConnector: AddressLookupConnector,
   val navigator: CompoundNavigator,
   authenticate: AuthAction,
   getData: DataRetrievalAction,
   requireData: DataRequiredAction,
   formProvider: PostcodeFormProvider,
   val controllerComponents: MessagesControllerComponents,
   val renderer: Renderer,
   common: CommonPostcodeService
)(implicit val ec: ExecutionContext) extends I18nSupport with NunjucksSupport with Retrievals {

  private def form: Form[String] = formProvider("partnershipEnterPreviousPostcode.required", "enterPostcode.invalid")

  def formWithError(messageKey: String): Form[String] = {
    form.withError("value", s"messages__error__postcode_$messageKey")
  }

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        common.get(getFormToJson(schemeName, index, mode), form)
      }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async{
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      retrieve(SchemeNameId) { schemeName =>
        common.post(
          getFormToJson(schemeName, index, mode),
          EnterPreviousPostCodeId(index),
          "enterPostcode.noresults",
          Some(mode),
          form
        )
      }
  }


  def getFormToJson(schemeName:String, index: Index, mode: Mode)(implicit request:DataRequest[AnyContent]): Form[String] => JsObject = {
    form => {
      val msg = request2Messages(request)
      val name = request.userAnswers.get(PartnershipDetailsId(index)).map(_.partnershipName).getOrElse(msg("establisherEntityTypePartnership"))
      Json.obj(
        "entityType" -> msg("establisherEntityTypePartnership"),
        "entityName" -> name,
        "form" -> form,
        "enterManuallyUrl" -> routes.ConfirmPreviousAddressController.onPageLoad(index,mode).url,
        "schemeName" -> schemeName,
        "h1MessageKey" -> "previousPostcode.title"
      )
    }
  }
}

