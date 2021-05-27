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

package controllers.benefitsAndInsurance

import config.AppConfig
import connectors.AddressLookupConnector
import controllers.actions._
import controllers.address.PostcodeController
import forms.address.PostcodeFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.benefitsAndInsurance.BenefitsInsuranceNameId

import javax.inject.Inject
import models.Mode
import models.requests.DataRequest
import navigators.{CompoundNavigator, Navigator}
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}

import scala.concurrent.ExecutionContext

class InsurerEnterPostcodeController @Inject()(val appConfig: AppConfig,
                                               override val messagesApi: MessagesApi,
                                               val addressLookupConnector: AddressLookupConnector,
                                               val navigator: CompoundNavigator,
                                               authenticate: AuthAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               formProvider: PostcodeFormProvider,
                                               val controllerComponents: MessagesControllerComponents
                                              )(implicit val ec: ExecutionContext) extends PostcodeController {

  val form: Form[String] = formProvider("","")

  def formWithError(messageKey: String): Form[String] = {
    form.withError("value", s"messages__error__postcode_$messageKey")
  }

  def onPageLoad(mode: Mode, srn: Option[String]): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      BenefitsInsuranceNameId.retrieve.right.map { name =>
          get(viewModel(mode, srn, name))
        }
    }

//  def viewModel(mode: Mode, srn: Option[String], name: String)(implicit request: DataRequest[AnyContent])
//  : PostcodeLookupViewModel =
//    PostcodeLookupViewModel(
//      postCall(mode, srn),
//      manualCall(mode, srn),
//      Messages("messages__insurer_enter_postcode__h1", Messages("messages__theInsuranceCompany")),
//      Messages("messages__insurer_enter_postcode__h1", name),
//      None,
//      srn = srn
//    )

  def getFormToJson(implicit request: DataRequest[AnyContent]): Form[String] => JsObject = {
    form =>
      Json.obj(
        "form" -> form(),
        "submitUrl" -> controllers.benefitsAndInsurance.routes.InsurerEnterPostcodeController.onSubmit.url,
        "enterManuallyUrl" -> None
      )
  }
  def onSubmit(mode: Mode, srn: Option[String]): Action[AnyContent] = (authenticate() andThen getData(mode, srn)
    andThen requireData).async {
    implicit request =>
      InsuranceCompanyNameId.retrieve.right.map { name =>
        post(InsurerEnterPostCodeId, viewModel(mode, srn, name), mode)
      }
  }

}
