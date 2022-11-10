/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.trustees.individual.details

import connectors.cache.UserAnswersCacheConnector
import controllers.ReasonController
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.ReasonFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.details.DirectorNoNINOReasonId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.details.TrusteeNoNINOReasonId
import models.requests.DataRequest
import models.{CheckMode, Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.DataUpdateService
import utils.UserAnswers
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TrusteeNoNINOReasonController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   val navigator: CompoundNavigator,
                                                   authenticate: AuthAction,
                                                   getData: DataRetrievalAction,
                                                   requireData: DataRequiredAction,
                                                   formProvider: ReasonFormProvider,
                                                   dataUpdateService: DataUpdateService,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                   val renderer: Renderer
                                                 )(implicit val executionContext: ExecutionContext)
  extends ReasonController {

  private def name(index: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(TrusteeNameId(index))
      .fold(Message("messages__trustee"))(_.fullName)

  private def form(index: Index)
                  (implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Message("messages__reason__error_ninoRequired", name(index)))

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            get(
              pageTitle     = Message("messages__whyNoNINO", Message("messages__individual")),
              pageHeading     = Message("messages__whyNoNINO", name(index)),
              isPageHeading = true,
              id            = TrusteeNoNINOReasonId(index),
              form          = form(index),
              schemeName    = schemeName
            )
        }
    }

    def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
      (authenticate andThen getData andThen requireData()).async {
        implicit request =>
          SchemeNameId.retrieve.map {
            schemeName =>
              form(index).bindFromRequest().fold(
                (formWithErrors: Form[_]) =>
                  renderer.render(
                    template = "reason.njk",
                    ctx = Json.obj(
                      "pageTitle"     -> Message("messages__whyNoNINO", Message("messages__individual")),
                      "pageHeading" -> Message("messages__whyNoNINO", name(index)),
                      "isPageHeading" -> true,
                      "form"          -> formWithErrors,
                      "schemeName"    -> schemeName
                    )
                  ).map(BadRequest(_)),
                value =>
                  for {
                    updatedAnswers <- Future.fromTry(setUpdatedAnswers(index, mode, value, request.userAnswers))
                    _              <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                  } yield
                    Redirect(navigator.nextPage(TrusteeNoNINOReasonId(index), updatedAnswers, mode))
              )
          }
      }

  private def setUpdatedAnswers(index: Index, mode: Mode, value: String, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          val directors = dataUpdateService.findMatchingDirectors(index)(ua)
          directors.foldLeft[UserAnswers](ua) { (acc, director) =>
            if (director.isDeleted)
              acc
            else
              acc.setOrException(DirectorNoNINOReasonId(director.mainIndex.get, director.index), value)
          }
        case _ => ua
      }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(TrusteeNoNINOReasonId(index), value)
    finalUpdatedUserAnswers
  }
}
