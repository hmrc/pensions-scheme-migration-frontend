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

package controllers.trustees

import controllers.Retrievals
import controllers.actions._
import forms.trustees.AddTrusteeFormProvider
import helpers.AddToListHelper
import identifiers.trustees.AddTrusteeId
import navigators.CompoundNavigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddTrusteeController @Inject()(override val messagesApi: MessagesApi,
                                         navigator: CompoundNavigator,
                                         authenticate: AuthAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: AddTrusteeFormProvider,
                                         helper: AddToListHelper,
                                         val controllerComponents: MessagesControllerComponents,
                                         renderer: Renderer
                                        )(implicit val ec: ExecutionContext)
  extends FrontendBaseController with Retrievals with I18nSupport with NunjucksSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        val trustees = request.userAnswers.allTrusteesAfterDelete
        val table = helper.mapTrusteesToTable(trustees)
        val json: JsObject = Json.obj(
          "form" -> formProvider(trustees),
          "table" -> table,
          "radios" -> Radios.yesNo(formProvider(trustees)("value")),
          "schemeName" -> existingSchemeName
        )
        renderer.render("trustees/addTrustee.njk", json).map(Ok(_))
    }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      val trustees = request.userAnswers.allTrusteesAfterDelete
      val table = helper.mapTrusteesToTable(trustees)

      formProvider(trustees).bindFromRequest().fold(
        formWithErrors => {
          val json: JsObject = Json.obj(
            "form" -> formWithErrors,
            "table" -> table,
            "radios" -> Radios.yesNo(formWithErrors("value")),
            "schemeName" -> existingSchemeName
          )
          renderer.render("trustees/addTrustee.njk", json).map(BadRequest(_))},
        value =>
          Future.successful(Redirect(navigator.nextPage(AddTrusteeId(value), request.userAnswers)))
      )
  }


}
