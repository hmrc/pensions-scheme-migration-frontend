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
import forms.address.PostcodeFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.address.EnterPostCodeId
import models.requests.DataRequest
import models.{Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.common.address.{CommonPostcodeService, CommonPostcodeTemplateData}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EnterPostcodeController @Inject()(
    val messagesApi: MessagesApi,
    authenticate: AuthAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: PostcodeFormProvider,
    common: CommonPostcodeService
)(implicit val ec: ExecutionContext) extends I18nSupport with Retrievals {

  private def form: Form[String] = formProvider("enterPostcode.required", "enterPostcode.invalid")

  def onPageLoad(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        common.get(getFormToTemplate(schemeName, establisherIndex, partnerIndex, mode), form)
      }
    }

  def onSubmit(establisherIndex: Index, partnerIndex: Index, mode: Mode):
  Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      retrieve(SchemeNameId) { schemeName =>
        common.post(
          getFormToTemplate(schemeName, establisherIndex, partnerIndex, mode),
          EnterPostCodeId(establisherIndex, partnerIndex),
          "enterPostcode.noresults",
          Some(mode),
          form
        )
      }
  }

  def getFormToTemplate(schemeName: String, establisherIndex: Index, partnerIndex: Index, mode: Mode
                       )(implicit request: DataRequest[AnyContent]): Form[String] => CommonPostcodeTemplateData = {
    val name: String = request.userAnswers.get(PartnerNameId(establisherIndex, partnerIndex))
      .map(_.fullName).getOrElse(Message("messages__partner"))
    val submitUrl = routes.EnterPostcodeController.onSubmit(establisherIndex, partnerIndex, mode)
    val enterManuallyUrl = routes.ConfirmAddressController.onPageLoad(establisherIndex, partnerIndex, mode).url

    form => {
      CommonPostcodeTemplateData(
        form,
        Message("messages__partner"),
        name,
        submitUrl,
        enterManuallyUrl,
        schemeName,
        "postcode.title"
      )
    }
  }
}

