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

package controllers.establishers

import controllers.Retrievals
import controllers.actions._
import forms.establishers.AddEstablisherFormProvider
import helpers.AddToListHelper
import identifiers.establishers.AddEstablisherId
import models.Establisher
import models.requests.DataRequest
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddEstablisherController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             navigator: CompoundNavigator,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: AddEstablisherFormProvider,
                                             helper: AddToListHelper,
                                             val controllerComponents: MessagesControllerComponents,
                                             renderer: Renderer
                                           )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Retrievals
    with I18nSupport
    with NunjucksSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        val allEstablishers = request.userAnswers.allEstablishersAfterDelete

        renderer.render(
          template = "establishers/addEstablisher.njk",
          ctx = getJson(formProvider(allEstablishers), allEstablishers)
        ).map(Ok(_))
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        val allEstablishers = request.userAnswers.allEstablishersAfterDelete

        formProvider(allEstablishers).bindFromRequest().fold(
          formWithErrors =>
            renderer.render(
              template = "establishers/addEstablisher.njk",
              ctx = getJson(formWithErrors, allEstablishers)
            ).map(BadRequest(_)),
          value =>
            Future.successful(Redirect(
              navigator.nextPage(
                id = AddEstablisherId(value),
                userAnswers = request.userAnswers
              )
            ))
        )
    }

  private def getJson(form: Form[_], establishers: Seq[Establisher[_]])(implicit request: DataRequest[AnyContent]): JsObject = {
    val establishersComplete = establishers.filter(_.isCompleted)
    val establishersIncomplete = establishers.filterNot(_.isCompleted)
    val completeTable = helper.mapEstablishersToList(establishersComplete,
      caption = "messages__schemeTaskList__completed", editLinkText = "site.change")
    val incompleteTable = helper.mapEstablishersToList(establishersIncomplete,
      caption = "site.incomplete", editLinkText = "site.add.details")
    Json.obj(
      "form" -> form,
      "itemListIncomplete" -> incompleteTable,
      "itemListComplete" -> completeTable,
      "radios" -> Radios.yesNo(form("value")),
      "schemeName" -> existingSchemeName
    )
  }
}
