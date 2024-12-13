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

package controllers.establishers.company.director

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.dataPrefill.DataPrefillRadioFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.director.TrusteeAlsoDirectorId
import models.{DataPrefillRadio, Index, entities}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DataPrefillService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{Enumerable, UserAnswers}
import views.html.DataPrefillRadioView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TrusteeAlsoDirectorController @Inject()(override val messagesApi: MessagesApi,
                                              userAnswersCacheConnector: UserAnswersCacheConnector,
                                              navigator: CompoundNavigator,
                                              authenticate: AuthAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              formProvider: DataPrefillRadioFormProvider,
                                              dataPrefillService: DataPrefillService,
                                              val controllerComponents: MessagesControllerComponents,
                                              dataPrefillRadioView: DataPrefillRadioView
                                             )(implicit val executionContext: ExecutionContext) extends FrontendBaseController
  with I18nSupport with Retrievals with Enumerable.Implicits {

  def onPageLoad(establisherIndex: Index): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      (CompanyDetailsId(establisherIndex) and SchemeNameId).retrieve.map { case companyName ~ schemeName =>
        implicit val ua: UserAnswers = request.userAnswers
        val seqTrustee = dataPrefillService.getListOfTrusteesToBeCopied(establisherIndex)
        if (seqTrustee.nonEmpty) {
          Future.successful(Ok(
            dataPrefillRadioView(
              form,
              Messages("messages__directors__prefill__title"),
              Messages("messages__directors__prefill__heading", companyName.companyName),
              DataPrefillRadio.radios(form, seqTrustee),
              schemeName,
              routes.TrusteeAlsoDirectorController.onSubmit(establisherIndex)
            )
          ))
        } else {
          Future(Redirect(controllers.common.routes.SpokeTaskListController.onPageLoad(establisherIndex, entities.Establisher, entities.Company)))
        }
      }
  }

  def onSubmit(establisherIndex: Index): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      implicit val ua: UserAnswers = request.userAnswers
      (CompanyDetailsId(establisherIndex) and SchemeNameId).retrieve.map { case companyName ~ schemeName =>
        val seqTrustee = dataPrefillService.getListOfTrusteesToBeCopied(establisherIndex)
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) => {
            Future.successful(BadRequest(
              dataPrefillRadioView(
                formWithErrors,
                Messages("messages__directors__prefill__title"),
                Messages("messages__directors__prefill__heading", companyName.companyName),
                DataPrefillRadio.radios(form, seqTrustee),
                schemeName,
                routes.TrusteeAlsoDirectorController.onSubmit(establisherIndex)
              )
            ))
          },
          value => {
            def uaAfterCopy: UserAnswers = if (value < 0) ua else
              dataPrefillService.copyAllTrusteesToDirectors(ua, Seq(value), establisherIndex)

            val updatedUa = uaAfterCopy.setOrException(TrusteeAlsoDirectorId(establisherIndex), value)
            userAnswersCacheConnector.save(request.lock, uaAfterCopy.data).map { _ =>
              Redirect(navigator.nextPage(TrusteeAlsoDirectorId(establisherIndex), updatedUa))
            }
          }
        )
      }
  }

  private def form: Form[Int] =
    formProvider("messages__directors__prefill__single__error__required")
}
