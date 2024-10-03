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

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.dataPrefill.DataPrefillCheckboxFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.{DirectorsAlsoTrusteesId, IsTrusteeNewId, TrusteeKindId}
import models.trustees.TrusteeKind
import models.{DataPrefillCheckbox, Index}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.DataPrefillService
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.MessageInterpolators
import utils.{Enumerable, TwirlMigration, UserAnswers}
import views.html.DataPrefillCheckboxView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DirectorsAlsoTrusteesController @Inject()(override val messagesApi: MessagesApi,
                                                userAnswersCacheConnector: UserAnswersCacheConnector,
                                                navigator: CompoundNavigator,
                                                authenticate: AuthAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                formProvider: DataPrefillCheckboxFormProvider,
                                                dataPrefillService: DataPrefillService,
                                                config: AppConfig,
                                                view: DataPrefillCheckboxView,
                                                val controllerComponents: MessagesControllerComponents,
                                                renderer: Renderer
                                               )(implicit val executionContext: ExecutionContext) extends FrontendBaseController
  with I18nSupport with Retrievals with Enumerable.Implicits with NunjucksSupport {

  private def form(implicit ua: UserAnswers, messages: Messages): Form[List[Int]] = {
    println(s"<<<<<<<<<<<<<<<<<<< ${ua.allTrusteesAfterDelete}")
    val existingTrusteeCount = ua.allTrusteesAfterDelete.size
    println(s">>>>>>>>>existingTrusteeCount $existingTrusteeCount")
    formProvider(existingTrusteeCount, "messages__trustees__prefill__multi__error__required",
      "messages__trustees__prefill__multi__error__noneWithValue",
      messages("messages__trustees__prefill__multi__error__moreThanTen", existingTrusteeCount, config.maxTrustees - existingTrusteeCount))
  }

  def onPageLoad(index: Index): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      SchemeNameId.retrieve.map { schemeName =>
        implicit val ua: UserAnswers = request.userAnswers
        val seqDirector = dataPrefillService.getListOfDirectorsToBeCopied
        val directorsIndexes = seqDirector.map(_.index).toList
        if (seqDirector.nonEmpty) {
          Future.successful(Ok(view(
            form,
            schemeName,
            "messages__trustees__prefill__heading",
            "messages__trustees__prefill__title",
            TwirlMigration.toTwirlCheckBoxes(DataPrefillCheckbox.checkboxes(form, seqDirector)),
            routes.DirectorsAlsoTrusteesController.onSubmit(index)
          )))
        } else {
          Future(Redirect(controllers.routes.TaskListController.onPageLoad))
        }
      }
  }

  def onSubmit(establisherIndex: Index): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      implicit val ua: UserAnswers = request.userAnswers
      SchemeNameId.retrieve.map { schemeName =>
        val seqDirector = dataPrefillService.getListOfDirectorsToBeCopied
        println(s">>>>>>>>>><<<<<<<<<<<<<<<<<<*******seqDirectors is $seqDirector")
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) => {
            Future.successful(BadRequest(view(
              formWithErrors,
              schemeName,
              "messages__trustees__prefill__title",
              "messages__trustees__prefill__heading",
              TwirlMigration.toTwirlCheckBoxes(DataPrefillCheckbox.checkboxes(form, seqDirector)),
              routes.DirectorsAlsoTrusteesController.onSubmit(establisherIndex)
            )))
          },
          value => {
            val uaAfterCopy = if (value.headOption.getOrElse(-1) < 0) ua else
              dataPrefillService.copyAllDirectorsToTrustees(ua, value,
                seqDirector.headOption.flatMap(_.mainIndex).getOrElse(0))

            val updatedUa = uaAfterCopy.setOrException(DirectorsAlsoTrusteesId(establisherIndex), value)
              .setOrException(IsTrusteeNewId(establisherIndex), value = true)
              .setOrException(TrusteeKindId(establisherIndex, TrusteeKind.Individual), TrusteeKind.Individual)
            userAnswersCacheConnector.save(request.lock, uaAfterCopy.data).map { _ =>
              Redirect(navigator.nextPage(DirectorsAlsoTrusteesId(establisherIndex), updatedUa))
            }
          }
        )
      }
  }
}
