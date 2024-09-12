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

package controllers.establishers.company.contact

import connectors.cache.UserAnswersCacheConnector
import controllers.{PhoneController, Retrievals}
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.PhoneFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.contact.EnterPhoneId
import models.requests.DataRequest
import models.{Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.common.phone.CommonPhoneService
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EnterPhoneController @Inject()(
                                      val messagesApi: MessagesApi,
                                      authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: PhoneFormProvider,
                                      common: CommonPhoneService
                                    )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport{

  private def name(index: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(CompanyDetailsId(index))
      .fold(Message("messages__company"))(_.companyName)

  private def form(index: Index)(implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Message("messages__enterPhone__error_required", name(index)))

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              entityName = name(index),
              entityType = Message("messages__company"),
              phoneId = EnterPhoneId(index),
              form = form(index),
              schemeName = schemeName,
              paragraphText = Seq(Message("messages__contact_details__phone__hint", name(index), schemeName))
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
              entityType = Message("messages__company"),
              phoneId = EnterPhoneId(index),
              form = form(index),
              schemeName = schemeName,
              paragraphText = Seq(Message("messages__contact_details__phone__hint", name(index), schemeName)),
              mode = Some(mode)
            )
        }
    }
}
