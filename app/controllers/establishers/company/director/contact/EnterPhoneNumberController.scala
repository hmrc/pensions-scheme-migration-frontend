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

package controllers.establishers.company.director.contact

import connectors.cache.UserAnswersCacheConnector
import controllers.PhoneController
import controllers.actions._
import forms.PhoneFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.contact.EnterPhoneId
import identifiers.trustees.individual.contact.{EnterPhoneId => trusteeEnterPhoneId}
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

class EnterPhoneNumberController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            val navigator: CompoundNavigator,
                                            authenticate: AuthAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            formProvider: PhoneFormProvider,
                                            dataUpdateService: DataUpdateService,
                                            val controllerComponents: MessagesControllerComponents,
                                            val userAnswersCacheConnector: UserAnswersCacheConnector,
                                            val renderer: Renderer
                                          )(implicit val executionContext: ExecutionContext)
  extends PhoneController {

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            get(
              entityName = name(establisherIndex, directorIndex),
              entityType = Messages("messages__director"),
              id = EnterPhoneId(establisherIndex, directorIndex),
              form = form(establisherIndex, directorIndex),
              schemeName = schemeName,
              paragraphText = Seq(Messages("messages__contact_details__hint", name(establisherIndex, directorIndex)))
            )

        }
    }

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            form(establisherIndex, directorIndex).bindFromRequest().fold(
              (formWithErrors: Form[_]) =>
                renderer.render(
                  template = "phone.njk",
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
                  Redirect(navigator.nextPage(EnterPhoneId(establisherIndex, directorIndex), updatedAnswers, mode))
            )
        }
    }

  private def name(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(DirectorNameId(establisherIndex, directorIndex))
      .fold("the director")(_.fullName)

  private def form(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Message("messages__enterPhone__error_required", name(establisherIndex, directorIndex)))

  private def setUpdatedAnswers(establisherIndex: Index, directorIndex: Index, mode: Mode, value: String, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
    mode match {
      case CheckMode =>
        dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
          ua.setOrException(trusteeEnterPhoneId(trustee.index), value)
        }.getOrElse(ua)
      case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(EnterPhoneId(establisherIndex, directorIndex), value)
    finalUpdatedUserAnswers
  }

}
