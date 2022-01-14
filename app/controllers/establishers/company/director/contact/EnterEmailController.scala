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

package controllers.establishers.company.director.contact

import connectors.cache.UserAnswersCacheConnector
import controllers.EmailAddressController
import controllers.actions._
import forms.EmailFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.contact.EnterEmailId
import identifiers.trustees.individual.contact.{EnterEmailId => trusteeEnterEmailId}
import models.requests.DataRequest
import models.{CheckMode, Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.DataUpdateService
import utils.UserAnswers
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class EnterEmailController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      val navigator: CompoundNavigator,
                                      authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: EmailFormProvider,
                                      dataUpdateService: DataUpdateService,
                                      val userAnswersCacheConnector: UserAnswersCacheConnector,
                                      val controllerComponents: MessagesControllerComponents,
                                      val renderer: Renderer
                                    )(implicit val executionContext: ExecutionContext)
  extends EmailAddressController {

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            get(
              entityName = name(establisherIndex, directorIndex),
              entityType = Messages("messages__director"),
              id = EnterEmailId(establisherIndex, directorIndex),
              form = form(establisherIndex, directorIndex),
              schemeName = schemeName,
              paragraphText = Seq(Messages("messages__contact_details__hint", name(establisherIndex, directorIndex)))
            )
        }
    }

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            form(establisherIndex, directorIndex).bindFromRequest().fold(
              (formWithErrors: Form[_]) =>
                renderer.render(
                  template = "email.njk",
                  ctx = Json.obj(
                    "entityName" -> name(establisherIndex, directorIndex),
                    "entityType" -> Messages("messages__director"),
                    "form" -> formWithErrors,
                    "schemeName" -> schemeName,
                    "paragraph" -> Seq(Messages("messages__contact_details__hint", name(establisherIndex, directorIndex)))
                  )
                ).map(BadRequest(_)),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(setUpdatedAnswers(establisherIndex, directorIndex, mode, value, request.userAnswers))
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield
                  Redirect(navigator.nextPage(EnterEmailId(establisherIndex, directorIndex), updatedAnswers, mode))
            )
        }
    }

  private def form(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Message("messages__enterEmail__error_required", name(establisherIndex, directorIndex)))

  private def name(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(DirectorNameId(establisherIndex, directorIndex))
      .fold("the director")(_.fullName)

  private def setUpdatedAnswers(establisherIndex: Index, directorIndex: Index, mode: Mode, value: String, ua: UserAnswers): Try[UserAnswers] = {
    var updatedUserAnswers: Try[UserAnswers] = Try(ua)
    if (mode == CheckMode) {
      val trustee = dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua)
      if (!trustee.isDeleted)
        updatedUserAnswers = ua.set(trusteeEnterEmailId(trustee.index), value)
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.get.set(EnterEmailId(establisherIndex, directorIndex), value)
    finalUpdatedUserAnswers
  }
}
