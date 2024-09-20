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

import controllers.Retrievals
import controllers.actions._
import forms.address.PostcodeFormProvider
import identifiers.adviser.{AdviserNameId, EnterPostCodeId}
import identifiers.beforeYouStart.SchemeNameId
import models.Mode
import models.requests.DataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.common.address.{CommonPostcodeService, CommonPostcodeTemplateData}
import viewmodels.Message
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EnterPostcodeController @Inject()(
    val messagesApi: MessagesApi,
    authenticate: AuthAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: PostcodeFormProvider,
    common: CommonPostcodeService
)(implicit val ec: ExecutionContext) extends I18nSupport with NunjucksSupport with Retrievals {

  private def form: Form[String] = formProvider("enterPostcode.required", "messages__adviser__enterPostcode__invalid")

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        common.get(getFormToTemplate(schemeName, mode), form)
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      retrieve(SchemeNameId) { schemeName =>
        common.post(getFormToTemplate(schemeName, mode), EnterPostCodeId, "messages__adviser__enterPostcode__no__results", form = form)
      }
  }

  def getFormToTemplate(schemeName: String, mode: Mode)(implicit request: DataRequest[AnyContent]): Form[String] => CommonPostcodeTemplateData = {
    val name: String = request.userAnswers.get(AdviserNameId).getOrElse(Message("messages__pension__adviser"))

    form => {
      CommonPostcodeTemplateData(
        form,
        Message("messages__pension__adviser"),
        name,
        controllers.adviser.routes.ConfirmAddressController.onPageLoad.url,
        schemeName
      )
    }
  }
}
