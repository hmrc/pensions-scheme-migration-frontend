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

package controllers.establishers.company.director.address

import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.PostcodeFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.address.EnterPostCodeId
import identifiers.trustees.individual.address.{EnterPostCodeId => trusteeEnterPostCodeId}
import models._
import models.requests.DataRequest
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{BadRequest, Redirect}
import play.api.mvc.{Action, AnyContent}
import renderer.Renderer
import services.DataUpdateService
import services.common.address.{CommonPostcodeService, CommonPostcodeTemplateData}
import viewmodels.Message
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class EnterPostcodeController @Inject()(
    val messagesApi: MessagesApi,
    userAnswersCacheConnector: UserAnswersCacheConnector,
    addressLookupConnector: AddressLookupConnector,
    navigator: CompoundNavigator,
    authenticate: AuthAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    dataUpdateService: DataUpdateService,
    formProvider: PostcodeFormProvider,
    renderer: Renderer,
    common: CommonPostcodeService
)(implicit val ec: ExecutionContext) extends I18nSupport with NunjucksSupport with Retrievals {

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        common.get(getFormToTemplate(schemeName, establisherIndex, directorIndex, mode), form)
      }
    }

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

        retrieve(SchemeNameId) { schemeName =>
          val formToTemplate: Form[String] => CommonPostcodeTemplateData = getFormToTemplate(schemeName, establisherIndex, directorIndex, mode)
          form.bindFromRequest().fold(
            formWithErrors =>
              renderer.render(common.viewTemplate, formToTemplate(formWithErrors)).map(BadRequest(_)),
            value =>
              addressLookupConnector.addressLookupByPostCode(value).flatMap {
                case Nil =>
                  renderer.render(common.viewTemplate, formToTemplate(formWithError("enterPostcode.noresults"))).map(BadRequest(_))

                case addresses =>
                  for {
                    updatedAnswers <- Future.fromTry(setUpdatedAnswers(establisherIndex, directorIndex, mode, addresses, request.userAnswers))
                    _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                  } yield {
                    val finalMode = Some(mode).getOrElse(NormalMode)
                    Redirect(navigator.nextPage(EnterPostCodeId(establisherIndex, directorIndex), updatedAnswers, finalMode))
                  }
              }
          )
        }
    }

  def getFormToTemplate(schemeName: String, establisherIndex: Index, directorIndex: Index, mode: Mode
                       )(implicit request: DataRequest[AnyContent]): Form[String] => CommonPostcodeTemplateData = {
    val name: String = request.userAnswers.get(DirectorNameId(establisherIndex, directorIndex))
      .map(_.fullName).getOrElse(Message("messages__director"))

    form => {

      CommonPostcodeTemplateData(
        form,
        Message("messages__director"),
        name,
        routes.ConfirmAddressController.onPageLoad(establisherIndex, directorIndex, mode).url,
        schemeName
      )
    }
  }

  private def setUpdatedAnswers(establisherIndex: Index, directorIndex: Index, mode: Mode, value: Seq[TolerantAddress], ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
    mode match {
      case CheckMode =>
        dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
          ua.setOrException(trusteeEnterPostCodeId(trustee.index), value)
        }.getOrElse(ua)
      case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(EnterPostCodeId(establisherIndex, directorIndex), value)
    finalUpdatedUserAnswers
  }

  private def formWithError(messageKey: String): Form[String] = {
    form.withError("value", s"messages__error__postcode_$messageKey")
  }

  private def form: Form[String] = formProvider("enterPostcode.required", "enterPostcode.invalid")
}
