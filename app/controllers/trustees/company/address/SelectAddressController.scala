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

package controllers.trustees.company.address

import controllers.actions._
import models.establishers.AddressPages
import controllers.trustees.company.address.routes.ConfirmAddressController
import forms.address.AddressListFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.company.address.{AddressId, AddressListId, EnterPostCodeId}
import models.{Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent}
import controllers.Retrievals
import services.common.address.CommonAddressListService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelectAddressController @Inject()(
  val messagesApi: MessagesApi,
  authenticate: AuthAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AddressListFormProvider,
  common:CommonAddressListService
)(implicit val ec: ExecutionContext) extends I18nSupport with NunjucksSupport with Retrievals {

  private def form: Form[Int] = formProvider("selectAddress.required")

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        getFormToJson(schemeName, index, mode).retrieve.map(common.get(_, form))
      }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      val addressPages: AddressPages = AddressPages(EnterPostCodeId(index), AddressListId(index), AddressId(index))
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      retrieve(SchemeNameId) { schemeName =>
        getFormToJson(schemeName, index, mode).retrieve.map(
          common.post(
            _,
            addressPages,
            manualUrlCall = ConfirmAddressController.onPageLoad(index,mode),
            mode = Some(mode),
            form = form
          ))
      }
    }

  def getFormToJson(schemeName:String, index: Index, mode: Mode) : Retrieval[Form[Int] => JsObject] =
    Retrieval(
      implicit request =>
        EnterPostCodeId(index).retrieve.map { addresses =>
          val msg = request2Messages(request)
          val name = request.userAnswers.get(CompanyDetailsId(index)).map(_.companyName).getOrElse(msg("messages__company"))

          form => Json.obj(
            "form" -> form,
            "addresses" -> common.transformAddressesForTemplate(addresses),
            "entityType" -> msg("messages__company"),
            "entityName" -> name,
            "enterManuallyUrl" -> ConfirmAddressController.onPageLoad(index,mode).url,
            "schemeName" -> schemeName
          )
        }
    )
}
