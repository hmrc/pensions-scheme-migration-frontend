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

package controllers.beforeYouStartSpoke

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.beforeYouStart.SchemeTypeFormProvider
import identifiers.beforeYouStart.{IsSchemeTypeOtherId, SchemeNameId, SchemeTypeId}
import models.SchemeType
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.beforeYouStart.SchemeTypeView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{TwirlMigration, UserAnswers}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SchemeTypeController @Inject()(
    override val messagesApi: MessagesApi,
    userAnswersCacheConnector: UserAnswersCacheConnector,
    navigator: CompoundNavigator,
    authenticate: AuthAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: SchemeTypeFormProvider,
    val controllerComponents: MessagesControllerComponents,
    schemeTypeView: SchemeTypeView
)(implicit val executionContext: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()) {
      implicit request =>
        SchemeNameId.retrieve.fold (
          _ => BadRequest("Error retrieving scheme name"),

          schemeName => {
            val preparedForm: Form[SchemeType] = request.userAnswers.get(SchemeTypeId)(SchemeType.optionalReads) match {
              case None => form
              case Some(value) => form.fill(value)
            }
            Ok(schemeTypeView(
              preparedForm,
              schemeName,
              routes.SchemeTypeController.onSubmit,
              TwirlMigration.toTwirlRadiosWithHintText(SchemeType.radios(preparedForm))
            ))
          }
      )
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) =>
            SchemeNameId.retrieve.map { schemeName =>
              Future.successful(BadRequest(
                schemeTypeView(
                  formWithErrors,
                  schemeName,
                  routes.SchemeTypeController.onSubmit,
                  TwirlMigration.toTwirlRadiosWithHintText(SchemeType.radios(formWithErrors))
                )
              ))
            },
          value => {
            val ua: Try[UserAnswers] = request.userAnswers.set(IsSchemeTypeOtherId, true).flatMap(_.set(SchemeTypeId, value))

            for {
              updatedAnswers <- Future.fromTry(ua)
              _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
            } yield Redirect(navigator.nextPage(SchemeTypeId, updatedAnswers))
          }
        )
    }
}
