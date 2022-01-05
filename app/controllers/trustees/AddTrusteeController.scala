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

package controllers.trustees

import config.AppConfig
import controllers.Retrievals
import controllers.actions._
import forms.trustees.AddTrusteeFormProvider
import helpers.AddToListHelper
import identifiers.beforeYouStart.SchemeTypeId
import identifiers.trustees.AddTrusteeId
import models.{SchemeType, Trustee}
import models.requests.DataRequest
import navigators.CompoundNavigator
import play.api.data.Form
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

        val json: JsObject = getJson(formProvider(trustees), trustees)
        renderer.render("trustees/addTrustee.njk", json).map(Ok(_))
    }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      def navNextPage(v: Option[Boolean]): Future[Result] =
        Future.successful(Redirect(navigator.nextPage(AddTrusteeId(v), request.userAnswers)))

      val trustees = request.userAnswers.allTrusteesAfterDelete
      val formWithErrors = formProvider(trustees).bindFromRequest()

      (formWithErrors.value, trustees.length) match {
        case (Some(v), _) => navNextPage(v)
        case (_, numberOfTrustees) if numberOfTrustees >= config.maxTrustees => navNextPage(None)
        case _ =>
          val json: JsObject = getJson(formWithErrors, trustees)
          renderer.render("trustees/addTrustee.njk", json).map(BadRequest(_))
      }
  }

  private def getJson(form: Form[_], trustees: Seq[Trustee[_]])(implicit request: DataRequest[AnyContent]): JsObject = {
    val trusteesComplete = trustees.filter(_.isCompleted)
    val trusteesIncomplete = trustees.filterNot(_.isCompleted)
    val hideDeleteLink = request.userAnswers.get(SchemeTypeId).contains(SchemeType.SingleTrust) && trustees.size == 1
    val completeTable = helper.mapTrusteesToTable(trusteesComplete,
      caption = "messages__schemeTaskList__completed", editLinkText = "site.change", hideDeleteLink)
    val incompleteTable = helper.mapTrusteesToTable(trusteesIncomplete,
      caption = "messages__schemeTaskList__incomplete", editLinkText = "site.add.details", hideDeleteLink)

    Json.obj(
      "form" -> form,
      "completeTable" -> completeTable,
      "incompleteTable" -> incompleteTable,
      "radios" -> Radios.yesNo(form("value")),
      "schemeName" -> existingSchemeName,
      "trusteeSize" -> trustees.size,
      "maxTrustees" -> config.maxTrustees
    )
  }
}
