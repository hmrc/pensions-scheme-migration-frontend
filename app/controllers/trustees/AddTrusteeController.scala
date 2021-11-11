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

import config.AppConfig
import controllers.Retrievals
import controllers.actions._
import forms.trustees.AddTrusteeFormProvider
import helpers.AddToListHelper
import identifiers.trustees.AddTrusteeId
import navigators.CompoundNavigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
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
                                         config: AppConfig,
                                         val controllerComponents: MessagesControllerComponents,
                                         renderer: Renderer
                                        )(implicit val ec: ExecutionContext)
  extends FrontendBaseController with Retrievals with I18nSupport with NunjucksSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        val trustees = request.userAnswers.allTrusteesAfterDelete
        val trusteesComplete = trustees.filter(_.isCompleted)
        val trusteesIncomplete = trustees.filterNot(_.isCompleted)
        val completeTable = helper.mapTrusteesToTable(trusteesComplete, caption = "Completed", editLinkText = "site.change")
        val incompleteTable = helper.mapTrusteesToTable(trusteesIncomplete, caption = "Incomplete", editLinkText = "site.add.details")

        println("\n\n\n completeTable : "+completeTable.rows.size)
        println("\n\n\n incompleteTable : "+incompleteTable.rows.size)

        val json: JsObject = Json.obj(
          "form" -> formProvider(trustees),
          "completeTable" -> completeTable,
          "incompleteTable" -> incompleteTable,
          "radios" -> Radios.yesNo(formProvider(trustees)("value")),
          "schemeName" -> existingSchemeName,
          "trusteeSize" -> trustees.size,
          "maxTrustees" -> config.maxTrustees
        )
        renderer.render("trustees/addTrustee.njk", json).map(Ok(_))
    }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      def navNextPage(v: Option[Boolean]):Future[Result] =
        Future.successful(Redirect(navigator.nextPage(AddTrusteeId(v), request.userAnswers)))

      val trustees = request.userAnswers.allTrusteesAfterDelete
      val trusteesComplete = trustees.filter(_.isCompleted)
      val trusteesIncomplete = trustees.filterNot(_.isCompleted)
      val completeTable = helper.mapEstablishersToTable(trusteesComplete, caption = "Completed", editLinkText = "site.change")
      val incompleteTable = helper.mapEstablishersToTable(trusteesIncomplete, caption = "Incomplete", editLinkText = "site.add.details")
      val formWithErrors = formProvider(trustees).bindFromRequest()

      (formWithErrors.value, trustees.length) match {
        case (Some(v), _) => navNextPage(v)
        case (_, numberOfTrustees) if numberOfTrustees >= config.maxTrustees => navNextPage(None)
        case _ =>
          val json: JsObject = Json.obj(
            "form" -> formWithErrors,
            "completeTable" -> completeTable,
            "incompleteTable" -> incompleteTable,
            "radios" -> Radios.yesNo(formWithErrors("value")),
            "schemeName" -> existingSchemeName,
            "trusteeSize" -> trustees.size,
            "maxTrustees" -> config.maxTrustees
          )
          renderer.render("trustees/addTrustee.njk", json).map(BadRequest(_))
      }
  }
}
