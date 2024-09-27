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

package controllers.trustees.individual.address

import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.PostcodeFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.{address => Director}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address.EnterPostCodeId
import models.requests.DataRequest
import models.{CheckMode, Index, Mode, TolerantAddress}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import renderer.Renderer
import services.DataUpdateService
import play.api.mvc.Results.{BadRequest, Redirect}
import services.common.address.{CommonPostcodeService, CommonPostcodeTemplateData}
import viewmodels.Message
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.UserAnswers
import views.html.address.PostcodeView

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
   formProvider: PostcodeFormProvider,
   dataUpdateService: DataUpdateService,
   renderer: Renderer,
   common: CommonPostcodeService,
   postcodeView: PostcodeView
)(implicit val ec: ExecutionContext) extends I18nSupport with NunjucksSupport with Retrievals {

  private def form: Form[String] = formProvider("enterPostcode.required", "enterPostcode.invalid")

  private def formWithError(messageKey: String): Form[String] = {
    form.withError("value", s"messages__error__postcode_$messageKey")
  }

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
        val formToTemplate: Form[String] => CommonPostcodeTemplateData = getFormToTemplate(schemeName, index, mode)
        form.bindFromRequest().fold(
          formWithErrors => {
            val templateData = formToTemplate(formWithErrors)
            Future.successful(BadRequest(postcodeView(
              formWithErrors,
              templateData.entityType,
              templateData.entityName,
              templateData.submitUrl,
              templateData.enterManuallyUrl,
              Some(templateData.schemeName)
            )))
          },
          value =>
            addressLookupConnector.addressLookupByPostCode(value).flatMap {
              case Nil =>
                val formWithErrors = formWithError("enterPostcode.noresults") // TODO Fix it
                val templateData = formToTemplate(formWithError("enterPostcode.noresults"))
                Future.successful(BadRequest(postcodeView(
                  formWithErrors,
                  templateData.entityType,
                  templateData.entityName,
                  templateData.submitUrl,
                  templateData.enterManuallyUrl,
                  Some(templateData.schemeName)
                )))
              case addresses =>
                for {
                  updatedAnswers <- Future.fromTry(setUpdatedAnswers(index, mode, addresses, request.userAnswers))
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield {
                  Redirect(navigator.nextPage(EnterPostCodeId(index), updatedAnswers, mode))
                }
            }
        )
      }
  }

  def getFormToTemplate(schemeName:String, index: Index, mode: Mode)(implicit request:DataRequest[AnyContent]): Form[String] => CommonPostcodeTemplateData = {
    val name: String = request.userAnswers.get(TrusteeNameId(index))
      .map(_.fullName).getOrElse(Message("trusteeEntityTypeIndividual"))
    val submitUrl = routes.EnterPostcodeController.onSubmit(index, mode)
    val enterManuallyUrl = routes.ConfirmAddressController.onPageLoad(index, mode).url

    form => {
      CommonPostcodeTemplateData(
        form,
        Message("trusteeEntityTypeIndividual"),
        name,
        submitUrl,
        enterManuallyUrl,
        schemeName
      )
    }
  }

  private def setUpdatedAnswers(index: Index, mode: Mode, value: Seq[TolerantAddress], ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          val directors = dataUpdateService.findMatchingDirectors(index)(ua)
          directors.foldLeft[UserAnswers](ua) { (acc, director) =>
            if (director.isDeleted)
              acc
            else
              acc.setOrException(Director.EnterPostCodeId(director.mainIndex.get, director.index), value)
          }
        case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(EnterPostCodeId(index), value)
    finalUpdatedUserAnswers
  }
}
