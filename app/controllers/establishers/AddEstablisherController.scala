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

package controllers.establishers

import controllers.Retrievals
import controllers.actions._
import forms.establishers.AddEstablisherFormProvider
import helpers.AddToListHelper
import identifiers.establishers.AddEstablisherId
import navigators.CompoundNavigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
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
        val establishers = request.userAnswers.allEstablishersAfterDelete
        val table        = helper.mapEstablishersToTable(establishers)

        renderer.render(
          template = "establishers/addEstablisher.njk",
          ctx = Json.obj(
            "form"       -> formProvider(establishers),
            "table"      -> table,
            "radios"     -> Radios.yesNo(formProvider(establishers)("value")),
            "schemeName" -> existingSchemeName
          )
        ).map(Ok(_))
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        val establishers = request.userAnswers.allEstablishersAfterDelete
        val table        = helper.mapEstablishersToTable(establishers)

        formProvider(establishers).bindFromRequest().fold(
          formWithErrors =>
            renderer.render(
              template = "establishers/addEstablisher.njk",
              ctx = Json.obj(
                "form"       -> formWithErrors,
                "table"      -> table,
                "radios"     -> Radios.yesNo(formWithErrors("value")),
                "schemeName" -> existingSchemeName
              )
            ).map(BadRequest(_)),
          value =>
            Future.successful(Redirect(
              navigator.nextPage(
                id          = AddEstablisherId(value),
                userAnswers = request.userAnswers
              )
            ))
        )
  }


}
