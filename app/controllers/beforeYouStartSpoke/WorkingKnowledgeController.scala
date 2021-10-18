/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.beforeYouStart.WorkingKnowledgeFormProvider
import identifiers.adviser._
import identifiers.beforeYouStart.WorkingKnowledgeId
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WorkingKnowledgeController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            userAnswersCacheConnector: UserAnswersCacheConnector,
                                            navigator: CompoundNavigator,
                                            authenticate: AuthAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            formProvider: WorkingKnowledgeFormProvider,
                                            val controllerComponents: MessagesControllerComponents,
                                            renderer: Renderer
                                          )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport with Retrievals with Enumerable.Implicits with NunjucksSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(WorkingKnowledgeId).fold(form)(form.fill)
      val json: JsObject = Json.obj(
        "form" -> preparedForm,
        "schemeName" -> existingSchemeName,
        "radios" -> Radios.yesNo(preparedForm("value"))
      )
      renderer.render("beforeYouStart/workingKnowledge.njk", json).map(Ok(_))
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) => {
          val json: JsObject = Json.obj(
            "form" -> formWithErrors,
            "schemeName" -> existingSchemeName,
            "radios" -> Radios.yesNo(formWithErrors("value"))
          )
          renderer.render("beforeYouStart/workingKnowledge.njk", json).map(BadRequest(_))
        },
          value =>
            for {
              updatedAnswers <- {
                val updatedUA = {
                  if (!value) {
                    request.userAnswers.removeAll(Set(EnterEmailId, EnterPhoneId, AdviserNameId,
                      EnterPostCodeId, AddressListId, AddressId))
                  }else{
                    request.userAnswers
                  }
                }
                  Future.fromTry(updatedUA.set(WorkingKnowledgeId, value))
              }
              _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
            } yield Redirect(navigator.nextPage(WorkingKnowledgeId, updatedAnswers))
      )
  }
}
