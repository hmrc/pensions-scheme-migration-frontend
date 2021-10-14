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
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.trustees.individual.TrusteeNameId
import models.Index
import models.requests.DataRequest
import models.trustees.TrusteeKind
import models.trustees.TrusteeKind.Individual
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AlreadyDeletedController @Inject()(override val messagesApi: MessagesApi,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          val controllerComponents: MessagesControllerComponents,
                                         renderer: Renderer
                                        )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with Retrievals with I18nSupport with Enumerable.Implicits {

  def onPageLoad(index: Index, trusteeKind: TrusteeKind): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        trusteeName(index, trusteeKind) match {
          case Right(trusteeName) =>
            renderer.render("alreadyDeleted.njk", json(trusteeName, existingSchemeName)).map(Ok(_))
          case Left(result) => result
        }
    }

  private def json(trusteeName: String, schemeName: Option[String])(implicit messages: Messages): JsObject = Json.obj(
    "title" -> messages("messages__alreadyDeleted__trustee_title"),
    "name" -> trusteeName,
    "schemeName" -> schemeName,
    "submitUrl" -> controllers.trustees.routes.AddTrusteeController.onPageLoad.url
  )

  private def trusteeName(index: Index, trusteeKind: TrusteeKind)(implicit
                                                                              dataRequest: DataRequest[AnyContent])
  : Either[Future[Result], String] = {
    trusteeKind match {
      case Individual => TrusteeNameId(index).retrieve.right.map(_.fullName)
      case _ => Right("Unimplemented functionality")
    }
  }
}
