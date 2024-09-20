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

package controllers.common

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import helpers.cya.{CYAHelper, CommonCYAHelper, MandatoryAnswerMissingException}
import identifiers.beforeYouStart.SchemeNameId
import models.entities.{EntityType, JourneyType, PensionManagementType}
import models.{CheckMode, Index, entities}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            authenticate: AuthAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            cyaHelper: CommonCYAHelper,
                                            val controllerComponents: MessagesControllerComponents,
                                            renderer: Renderer
                                          )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Enumerable.Implicits
    with I18nSupport
    with Retrievals {

  private val logger = Logger(classOf[CheckYourAnswersController])
  def onPageLoad(index: Index,
                 pensionManagementType: PensionManagementType,
                 entityType: EntityType,
                 journeyType: JourneyType): Action[AnyContent] = page(index, pensionManagementType, entityType, None, journeyType)

  def onPageLoadWithRepresentative(index: Index,
                                   pensionManagementType: PensionManagementType,
                                   entityType: EntityType,
                                   entityRepresentativeIndex: Index): Action[AnyContent] =
    page(index, pensionManagementType, entityType, Some(entityRepresentativeIndex), entities.Details)

  private def page(index: Index,
                   pensionManagementType: PensionManagementType,
                   entityType: EntityType,
                   entityRepresentativeIndex: Option[Index],
                   journeyType: JourneyType): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      val continueUrl = {
        entityRepresentativeIndex match {
          case Some(entityRepresentativeIndex) =>
            entityType match {
              case entities.Company => controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(index, CheckMode).url
              case entities.Partnership => controllers.establishers.partnership.routes.AddPartnersController.onPageLoad(index, CheckMode).url
              case entities.Individual => throw new RuntimeException("Entity Representative can't be individual")
            }
          case None => controllers.common.routes.SpokeTaskListController.onPageLoad(index, pensionManagementType, entityType).url
        }

      }

      Try {
        val rows = cyaHelper.rows(index, pensionManagementType, entityType, entityRepresentativeIndex, journeyType)
        val schemeName = CYAHelper.getAnswer(SchemeNameId)(request.userAnswers, implicitly)
        (rows, schemeName)
      } match {
        case Success((rows, schemeName)) =>
          renderer.render(
            template = "check-your-answers.njk",
            ctx = Json.obj(
              "list" -> rows,
              "schemeName" -> schemeName,
              "submitUrl" -> continueUrl
            )
          ).map(Ok(_))
        case Failure(ex: MandatoryAnswerMissingException) =>
          logger.warn(s"MandatoryAnswerMissingException:  ${ex.getMessage}")
          renderer.render("badRequest.njk").map(BadRequest(_))
        case Failure(ex) =>
          logger.warn(s"Unexpected error occurred: ${ex.getMessage}")
          renderer.render("internalServerError.njk").map(InternalServerError(_))
      }
    }
}
