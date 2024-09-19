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

package controllers.establishers.individual.address

import controllers.actions._
import models.establishers.AddressPages
import forms.address.AddressListFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address.{EnterPreviousPostCodeId, PreviousAddressId, PreviousAddressListId}
import models.{Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import controllers.Retrievals
import services.common.address.{CommonAddressListTemplateData, CommonAddressListService}
import uk.gov.hmrc.nunjucks.NunjucksSupport
import controllers.establishers.individual.address.routes.ConfirmPreviousAddressController
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
)(implicit val ec: ExecutionContext) extends I18nSupport with NunjucksSupport with Retrievals {

  private def form: Form[Int] = formProvider("selectAddress.required")

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        getFormToTemplate(schemeName, index, mode).retrieve.map(formToTemplate => common.get(formToTemplate(form)))
      }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      val addressPages: AddressPages = AddressPages(EnterPreviousPostCodeId(index), PreviousAddressListId(index), PreviousAddressId(index))
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      retrieve(SchemeNameId) { schemeName =>
        getFormToTemplate(schemeName, index, mode).retrieve.map(
          common.post(
            _,
            addressPages,
            manualUrlCall = ConfirmPreviousAddressController.onPageLoad(index,mode),
            form = form
          ))
      }
    }

  def getFormToTemplate(schemeName:String, index: Index, mode: Mode) : Retrieval[Form[Int] => CommonAddressListTemplateData] =
    Retrieval(
      implicit request =>
        EnterPreviousPostCodeId(index).retrieve.map { addresses =>
          val name: String = request.userAnswers.get(EstablisherNameId(index))
            .map(_.fullName).getOrElse(Message("establisherEntityTypeIndividual"))

          form =>
            CommonAddressListTemplateData(
              form,
              common.transformAddressesForTemplate(addresses),
              Message("establisherEntityTypeIndividual"),
              name,
              ConfirmPreviousAddressController.onPageLoad(index, mode).url,
              schemeName,
              "previousAddressList.title"
            )
        }
    )
}
