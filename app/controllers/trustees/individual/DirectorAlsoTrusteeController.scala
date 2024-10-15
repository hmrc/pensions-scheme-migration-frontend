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

package controllers.trustees.individual

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.dataPrefill.DataPrefillRadioFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.{DirectorAlsoTrusteeId, IsTrusteeNewId, TrusteeKindId}
import models.trustees.TrusteeKind
import models.{DataPrefillRadio, Index}
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

class DirectorAlsoTrusteeController @Inject()(override val messagesApi: MessagesApi,
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

  private def form: Form[Int] =
    formProvider("messages__trustees__prefill__single__error__required")

  def onPageLoad(index: Index): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      SchemeNameId.retrieve.map { schemeName =>
        implicit val ua: UserAnswers = request.userAnswers
        val seqDirector = dataPrefillService.getListOfDirectorsToBeCopied

        if (seqDirector.nonEmpty) {
          Future.successful(Ok(
            dataPrefillRadioView(
              form,
              Messages("messages__trustees__prefill__title"),
              Messages("messages__trustees__prefill__heading"),
              DataPrefillRadio.radios(form, seqDirector),
              schemeName,
              routes.DirectorAlsoTrusteeController.onSubmit(index)
            )
          ))
        } else {
          Future(Redirect(controllers.routes.TaskListController.onPageLoad))
        }
      }
  }

  def onSubmit(index: Index): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      implicit val ua: UserAnswers = request.userAnswers
      SchemeNameId.retrieve.map { schemeName =>
        val seqDirector = dataPrefillService.getListOfDirectorsToBeCopied
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) => {
            Future.successful(BadRequest(
              dataPrefillRadioView(
                formWithErrors,
                Messages("messages__trustees__prefill__title"),
                Messages("messages__trustees__prefill__heading"),
                DataPrefillRadio.radios(form, seqDirector),
                schemeName,
                routes.DirectorAlsoTrusteeController.onSubmit(index)
              )
            ))
          },
          value => {
            val uaAfterCopy = if (value < 0) ua else dataPrefillService.copyAllDirectorsToTrustees(ua, Seq(value),
              seqDirector.headOption.flatMap(_.mainIndex).getOrElse(0))
            val updatedUa = uaAfterCopy.setOrException(DirectorAlsoTrusteeId(index), value)
              .setOrException(IsTrusteeNewId(index), value = true)
              .setOrException(TrusteeKindId(index, TrusteeKind.Individual), TrusteeKind.Individual)
            userAnswersCacheConnector.save(request.lock, uaAfterCopy.data).map { _ =>
              Redirect(navigator.nextPage(DirectorAlsoTrusteeId(index), updatedUa))
            }
          }
        )
      }
  }
}
