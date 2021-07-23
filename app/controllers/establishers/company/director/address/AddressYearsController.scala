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

package controllers.establishers.company.director.address

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.establishers.address.AddressYearsFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.address.AddressYearsId
import models.Index
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddressYearsController @Inject()(override val messagesApi: MessagesApi,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       navigator: CompoundNavigator,
                                       formProvider: AddressYearsFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer)(implicit ec: ExecutionContext)
  extends FrontendBaseController  with I18nSupport with Retrievals with Enumerable.Implicits with NunjucksSupport {

  private def form: Form[Boolean] =
    formProvider()

  def onPageLoad(establisherIndex: Index, directorIndex: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (DirectorNameId(establisherIndex, directorIndex) and SchemeNameId).retrieve.right.map { case directorName ~ schemeName =>
        val preparedForm = request.userAnswers.get(AddressYearsId(establisherIndex, directorIndex)) match {
          case Some(value) => form.fill(value)
          case None        => form
        }
        val json = Json.obj(
          "schemeName" -> schemeName,
          "entityName" -> directorName.fullName,
          "entityType" -> Messages("messages__director"),
          "form" -> preparedForm,
          "radios" -> Radios.yesNo (preparedForm("value"))
        )
        renderer.render("address/addressYears.njk", json).map(Ok(_))
      }
    }

  def onSubmit(establisherIndex: Index, directorIndex: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (DirectorNameId(establisherIndex, directorIndex) and SchemeNameId).retrieve.right.map { case directorName ~ schemeName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val json = Json.obj(
                "schemeName" -> schemeName,
                "entityName" -> directorName.fullName,
                "entityType" -> Messages("messages__director"),
                "form" -> formWithErrors,
                "radios" -> Radios.yesNo(form("value"))
              )

              renderer.render("address/addressYears.njk", json).map(BadRequest(_))
            },
            value => {
              val updatedUA = request.userAnswers.setOrException(AddressYearsId(establisherIndex, directorIndex), value)
              userAnswersCacheConnector.save(request.lock, updatedUA.data).map { _ =>
                Redirect(navigator.nextPage(AddressYearsId(establisherIndex, directorIndex), updatedUA))
              }
            }
          )
        }
    }

}
