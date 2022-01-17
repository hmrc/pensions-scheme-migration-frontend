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

package controllers.trustees.individual.contact

import connectors.cache.UserAnswersCacheConnector
import controllers.PhoneController
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.PhoneFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.{contact => Director}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.contact.EnterPhoneId
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

class EnterPhoneController @Inject()(
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

  private def name(index: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(TrusteeNameId(index))
      .fold("the trustee")(_.fullName)

  private def form(index: Index)(implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Messages("messages__enterPhone__error_required", name(index)))

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            get(
              entityName = name(index),
              entityType = Messages("messages__individual"),
              id = EnterPhoneId(index),
              form = form(index),
              schemeName = schemeName,
              paragraphText = Seq(Messages("messages__contact_details__hint", name(index)))
            )
        }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            form(index).bindFromRequest().fold(
              (formWithErrors: Form[_]) =>
                renderer.render(
                  template = "phone.njk",
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
                  Redirect(navigator.nextPage(EnterPhoneId(index), updatedAnswers, mode))
            )
        }
    }

  private def setUpdatedAnswers(index: Index, mode: Mode, value: String, ua: UserAnswers): Try[UserAnswers] = {
    var updatedUserAnswers: Try[UserAnswers] = Try(ua)
    if (mode == CheckMode) {
      val directors = dataUpdateService.findMatchingDirectors(index)(ua)
      for(director <- directors) {
        if (!director.isDeleted)
          updatedUserAnswers = updatedUserAnswers.get.set(Director.EnterPhoneId(director.mainIndex.get, director.index), value)
      }
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.get.set(EnterPhoneId(index), value)
    finalUpdatedUserAnswers
  }
}
