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

package controllers.adviser

import controllers.actions._
import models.establishers.AddressPages
import controllers.adviser.routes.ConfirmAddressController
import forms.address.AddressListFormProvider
import identifiers.adviser.{AddressId, AddressListId, AdviserNameId, EnterPostCodeId}
import identifiers.beforeYouStart.SchemeNameId
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import controllers.Retrievals
import services.common.address.{CommonAddressListService, CommonAddressListTemplateData}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.Message

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

  def onPageLoad(): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        getFormToTemplate(schemeName).retrieve.map(formToTemplate => common.get(formToTemplate(form)))
      }
    }

  def onSubmit(): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      val addressPages: AddressPages = AddressPages(EnterPostCodeId, AddressListId, AddressId)
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      retrieve(SchemeNameId) { schemeName =>
        getFormToTemplate(schemeName).retrieve.map(
          common.post(
            _,
            addressPages,
            manualUrlCall = ConfirmAddressController.onPageLoad,
            form = form
          ))
      }
    }

  def getFormToTemplate(schemeName: String): Retrieval[Form[Int] => CommonAddressListTemplateData] =
    Retrieval(
      implicit request =>
        EnterPostCodeId.retrieve.map { addresses =>
          val name: String = request.userAnswers.get(AdviserNameId).getOrElse(Message("messages__pension__adviser"))

          form =>
            CommonAddressListTemplateData(
              form,
              common.transformAddressesForTemplate(addresses),
              Message("messages__pension__adviser"),
              name,
              ConfirmAddressController.onPageLoad.url,
              schemeName
            )
        }
    )
}
