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

import config.AppConfig
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.PostcodeFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address.EnterPostCodeId
import models.requests.DataRequest
import models.{Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.common.address.{CommonPostcodeService, CommonPostcodeTemplateData}
import viewmodels.Message
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EnterPostcodeController @Inject()(
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

  private def form: Form[String] = formProvider("enterPostcode.required", "enterPostcode.invalid")

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        common.get(getFormToTemplate(schemeName, index, mode), form)
      }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async{
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      retrieve(SchemeNameId) { schemeName =>
        common.post(getFormToTemplate(schemeName, index, mode), EnterPostCodeId(index), "enterPostcode.noresults",Some(mode), form)
      }
  }

  def getFormToTemplate(schemeName:String, index: Index, mode: Mode
                       )(implicit request:DataRequest[AnyContent]): Form[String] => CommonPostcodeTemplateData = {
    val name = request.userAnswers.get(EstablisherNameId(index))
      .map(_.fullName).getOrElse(Message("establisherEntityTypeIndividual").resolve)

    form => {
      CommonPostcodeTemplateData(
        form,
        Message("establisherEntityTypeIndividual").resolve,
        name,
        controllers.establishers.individual.address.routes.ConfirmAddressController.onPageLoad(index,mode).url,
        schemeName
      )
    }
  }
}
