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

package controllers.trustees.individual.contact

import connectors.cache.UserAnswersCacheConnector
import controllers.EmailAddressController
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.EmailFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.{contact => Director}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.contact.EnterEmailId
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
                                            val controllerComponents: MessagesControllerComponents,
                                            val userAnswersCacheConnector: UserAnswersCacheConnector,
                                            val renderer: Renderer
                                          )(implicit val executionContext: ExecutionContext)
  extends EmailAddressController {

  private def name(index: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(TrusteeNameId(index))
      .fold("the trustee")(_.fullName)

  private def form(index: Index)(implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Messages("messages__enterEmail__error_required", name(index)))

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            get(
              entityName = name(index),
              entityType = Messages("messages__individual"),
              id            = EnterEmailId(index),
              form          = form(index),
              schemeName    = schemeName,
              paragraphText = Seq(Messages("messages__contact_details__hint", name(index)))
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
                  template = "email.njk",
                  ctx = Json.obj(
                    "entityName" -> name(index),
                    "entityType" -> Messages("messages__individual"),
                    "form" -> formWithErrors,
                    "schemeName" -> schemeName,
                    "paragraph" -> Seq(Messages("messages__contact_details__hint", name(index)))
                  )
                ).map(BadRequest(_)),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(setUpdatedAnswers(index, mode, value, request.userAnswers))
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield
                  Redirect(navigator.nextPage(EnterEmailId(index), updatedAnswers, mode))
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
              acc.setOrException(Director.EnterEmailId(director.mainIndex.get, director.index), value)
          }
        case _ => ua
      }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(EnterEmailId(index), value)
    finalUpdatedUserAnswers
  }
}
