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

package controllers.trustees

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.partnership.PartnershipDetailsId
import models.Index
import models.requests.DataRequest
import models.trustees.TrusteeKind
import models.trustees.TrusteeKind.{Company, Individual, Partnership}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Enumerable
import views.html.AlreadyDeletedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AlreadyDeletedController @Inject()(override val messagesApi: MessagesApi,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          val controllerComponents: MessagesControllerComponents,
                                         alreadyDeletedView: AlreadyDeletedView
                                        )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with Retrievals with I18nSupport with Enumerable.Implicits {

  def onPageLoad(index: Index, trusteeKind: TrusteeKind): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        trusteeName(index, trusteeKind) match {
          case Right(trusteeName) =>
            Future.successful(Ok(alreadyDeletedView(
              "messages__alreadyDeleted__trustee_title",
              trusteeName,
              existingSchemeName,
              controllers.trustees.routes.AddTrusteeController.onPageLoad.url
            )))
          case Left(result) => result
        }
    }

  private def trusteeName(index: Index, trusteeKind: TrusteeKind)(implicit
                                                                              dataRequest: DataRequest[AnyContent])
  : Either[Future[Result], String] = {
    trusteeKind match {
      case Individual => TrusteeNameId(index).retrieve.map(_.fullName)
      case Company => CompanyDetailsId(index).retrieve.map(_.companyName)
      case Partnership => PartnershipDetailsId(index).retrieve.map(_.partnershipName)
      case null => Right("Unimplemented functionality")
    }
  }
}
