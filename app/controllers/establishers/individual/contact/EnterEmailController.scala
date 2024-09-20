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

package controllers.establishers.individual.contact

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.EmailFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.contact.EnterEmailId
import models.requests.DataRequest
import models.{Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.common.contact.CommonEmailAddressService
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EnterEmailController @Inject()(
                                      val messagesApi: MessagesApi,
                                      authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: EmailFormProvider,
                                      common: CommonEmailAddressService
                                    )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              entityName = name(index),
              entityType = Message("messages__individual"),
              emailId = EnterEmailId(index),
              form = form(index),
              schemeName = schemeName,
              paragraphText = Seq(Message("messages__contact_details__hint", name(index)))
            )
        }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              entityName = name(index),
              entityType = Message("messages__individual"),
              emailId = EnterEmailId(index),
              form = form(index),
              schemeName = schemeName,
              paragraphText = Seq(Message("messages__contact_details__hint", name(index))),
              mode = Some(mode)
            )
        }
    }

  private def form(index: Index)(implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Message("messages__enterEmail__error_required", name(index)))

  private def name(index: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(EstablisherNameId(index))
      .fold(Message("messages__individual"))(_.fullName)
}
