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
import helpers.SpokeCreationService
import identifiers.beforeYouStart.SchemeNameId
import identifiers.identifierUtils
import models.entities._
import models.requests.DataRequest
import models.{EntitySpoke, Index, entities}
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsObject, Json, OFormat}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{entityTypeError, managementTypeError}
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
class SpokeTaskListController @Inject() (
                                          val controllerComponents: MessagesControllerComponents,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          spokeCreationService: SpokeCreationService,
                                          renderer: Renderer
                                        )
                                        (implicit val ec: ExecutionContext) extends FrontendBaseController
  with I18nSupport
  with Retrievals
  with NunjucksSupport {

  //scalastyle:off
  private def getTemplateData(index: Index,
                                  pensionManagementType: PensionManagementType,
                                  entityType: EntityType,
                                  schemeName: String)(implicit request: DataRequest[_]) = {



    val emptyEntityNameMsg = entityType match {
      case Company => Message("messages__company")
      case Individual => Message("messages__individual")
      case Partnership => Message("messages__partnership")
      case e => entityTypeError(e)
    }

    val managementTypeTypeMsg = pensionManagementType match {
      case Establisher => Message("messages__tasklist__establisher")
      case Trustee => Message("messages__tasklist__trustee")
      case e => managementTypeError(e)
    }

    val entityName = identifierUtils.getNameOfEntityType(index, pensionManagementType, entityType, emptyEntityNameMsg)

    val spokes = spokeCreationService.getSpokes(pensionManagementType, entityType, request.userAnswers, entityName, index)

    val totalSpokes = spokes.size
    val completedCount = spokes.count(_.isCompleted.contains(true))

    val submitUrl = {
      def defaultRoute = {
        pensionManagementType match {
          case entities.Establisher => controllers.establishers.routes.AddEstablisherController.onPageLoad
          case entities.Trustee => controllers.trustees.routes.AddTrusteeController.onPageLoad
          case e => managementTypeError(e)
        }

      }
      entityType match {

        case entities.Company =>
          entityName match {
            case name if name == emptyEntityNameMsg =>
              pensionManagementType match {
                case entities.Establisher => controllers.establishers.company.routes.CompanyDetailsController.onPageLoad(index)
                case entities.Trustee => controllers.trustees.company.routes.CompanyDetailsController.onPageLoad(index)
                case e => managementTypeError(e)
              }
            case _ => defaultRoute
          }
        case _ => defaultRoute
      }
    }

    TemplateDataFull(
      spokes,
      entityName,
      schemeName,
      managementTypeTypeMsg.resolve,
      submitUrl.url,
      totalSpokes,
      completedCount
    )
  }
  private case class TemplateDataFull(
                                   taskSections: Seq[EntitySpoke],
                                   entityName: String,
                                   schemeName: String,
                                   entityType: String,
                                   submitUrl: String,
                                   totalSpokes: Int,
                                   completedCount: Int
                                 )

  implicit private val templateFormat: OFormat[TemplateDataFull] = Json.format[TemplateDataFull]
  def onPageLoad(index: Index, pensionManagementType: PensionManagementType, entityType: EntityType): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      SchemeNameId.retrieve.map { schemeName =>
        template(getTemplateData(index, pensionManagementType, entityType,schemeName)).map {Ok(_)}
      }
    }


  private def template(
           data: TemplateDataFull
         )(implicit request: DataRequest[AnyContent]): Future[Html] = {

    renderer.render(
      template = "spokeTaskList.njk",
      ctx = Json.toJson(data).as[JsObject]
    )
  }
}
