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

package controllers.establishers.partnership.partner.address

import controllers.Retrievals
import controllers.actions._
import forms.address.AddressListFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.address.{EnterPreviousPostCodeId, PreviousAddressId, PreviousAddressListId}
import models.establishers.AddressPages
import models.{Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.common.address.{CommonAddressListService, CommonAddressListTemplateData}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelectPreviousAddressController @Inject()(
    val messagesApi: MessagesApi,
    authenticate: AuthAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: AddressListFormProvider,
    common:CommonAddressListService
 )(implicit val ec: ExecutionContext) extends I18nSupport with Retrievals {

  private def form: Form[Int] = formProvider("selectAddress.required")

  def onPageLoad(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        retrieve(SchemeNameId) { schemeName =>
          getFormToTemplate(schemeName, establisherIndex, partnerIndex, mode)
            .retrieve.map(formToTemplate =>
              common.get(
                formToTemplate(form),
                form,
                submitUrl = routes.SelectPreviousAddressController.onSubmit(establisherIndex, partnerIndex, mode)
              ))
        }
    }

  def onSubmit(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>

      val addressPages: AddressPages = AddressPages(EnterPreviousPostCodeId(establisherIndex, partnerIndex),
        PreviousAddressListId(establisherIndex, partnerIndex), PreviousAddressId(establisherIndex, partnerIndex))
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      retrieve(SchemeNameId) { schemeName =>
        getFormToTemplate(schemeName, establisherIndex, partnerIndex, mode).retrieve.map(
          common.post(
            _,
            addressPages,
            Some(mode),
            routes.ConfirmPreviousAddressController.onPageLoad(establisherIndex, partnerIndex, mode),
            form = form,
            submitUrl = routes.SelectPreviousAddressController.onSubmit(establisherIndex, partnerIndex, mode)
          ))
      }
    }

  def getFormToTemplate(schemeName: String,
                        establisherIndex: Index,
                        partnerIndex: Index,
                        mode: Mode): Retrieval[Form[Int] => CommonAddressListTemplateData] =
    Retrieval(
      implicit request =>
        EnterPreviousPostCodeId(establisherIndex, partnerIndex).retrieve.map { addresses =>
          val name: String = request.userAnswers.get(PartnerNameId(establisherIndex, partnerIndex))
            .map(_.fullName).getOrElse(Message("messages__partner"))

          form =>
            CommonAddressListTemplateData(
              form,
              addresses,
              Message("messages__partner"),
              name,
              routes.ConfirmPreviousAddressController.onPageLoad(establisherIndex, partnerIndex, mode).url,
              schemeName,
              "previousAddressList.title"
            )
        }
    )
}

