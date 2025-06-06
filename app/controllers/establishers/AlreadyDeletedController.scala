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

package controllers.establishers

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import models.Index
import models.establishers.EstablisherKind
import models.establishers.EstablisherKind.{Company, Individual, Partnership}
import models.requests.DataRequest
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

  def onPageLoad(index: Index, establisherKind: EstablisherKind): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        establisherName(index, establisherKind) match {
          case Right(establisherName) =>
            Future.successful(Ok(alreadyDeletedView(
              "messages__alreadyDeleted__establisher_title",
              establisherName,
              existingSchemeName,
              controllers.establishers.routes.AddEstablisherController.onPageLoad.url
            )))
          case Left(result) => result
        }
    }

  private def establisherName(index: Index, establisherKind: EstablisherKind)(implicit
                                                                              dataRequest: DataRequest[AnyContent])
  : Either[Future[Result], String] = {
    establisherKind match {
      case Individual => EstablisherNameId(index).retrieve.map(_.fullName)
      case Company => CompanyDetailsId(index).retrieve.map(_.companyName)
      case Partnership => PartnershipDetailsId(index).retrieve.map(_.partnershipName)
      case null => Right("Unimplemented functionality")
    }
  }
}
