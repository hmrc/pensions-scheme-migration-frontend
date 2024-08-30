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
import identifiers.TypedIdentifier
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.individual.TrusteeNameId
import models.entities._
import models.requests.DataRequest
import models.{EntitySpoke, Index}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.{JsObject, Json, OFormat, Reads}
import play.api.mvc.{AnyContent, Call, MessagesControllerComponents}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
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

  private case class TemplateData(spokes: Seq[EntitySpoke],
                                  entityName: String,
                                  managementTypeTypeMsg: Message,
                                  submitUrl: Call) {
    def toFull(schemeName: String)(implicit messages: Messages): TemplateDataFull = {
      val totalSpokes = spokes.size
      val completedCount = spokes.count(_.isCompleted.contains(true))
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
  def onPageLoad(index: Index, pensionManagementType: PensionManagementType, entityType: EntityType) =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      val data = getTemplateData(index, pensionManagementType, entityType)
      SchemeNameId.retrieve.map { schemeName =>
        template(data.toFull(schemeName)).map {Ok(_)}
      }
    }

  private def getTemplateData(index: Index,
                                   pensionManagementType: PensionManagementType,
                                   entityType: EntityType)
                                  (implicit request: DataRequest[_]) = {

    def getEntityName[T](typedIdentifier: TypedIdentifier[T],
                      emptyNameMessage: Message)
                      (block: T => Message)
                      (implicit request: DataRequest[_],
                       reads: Reads[T]): Message = {
      request.userAnswers
        .get(typedIdentifier)
        .fold(emptyNameMessage)(block)
    }

    pensionManagementType match {
      case Establisher => ???
      case Trustee =>
        entityType match {
          case Company =>
            val emptyEntityName = Message("messages__company")
            val entityName = getEntityName(
              CompanyDetailsId(index), emptyEntityName
            )(_.companyName)
            TemplateData(
              spokes = spokeCreationService.getTrusteeCompanySpokes(
                request.userAnswers,
                entityName,
                index
              ),
              entityName,
              managementTypeTypeMsg = Message("messages__tasklist__trustee"),
              submitUrl = entityName match {
                case name if name == emptyEntityName =>
                  controllers.trustees.company.routes.CompanyDetailsController.onPageLoad(index)
                case _ => controllers.trustees.routes.AddTrusteeController.onPageLoad
              }
            )
          case Individual =>
            val emptyEntityName = Message("messages__individual")
            val entityName = getEntityName(
              TrusteeNameId(index), emptyEntityName
            )(_.fullName)
            TemplateData(
              spokes = spokeCreationService.getTrusteeIndividualSpokes(
                request.userAnswers,
                entityName,
                index
              ),
              entityName,
              managementTypeTypeMsg = Message("messages__tasklist__trustee"),
              submitUrl = controllers.trustees.routes.AddTrusteeController.onPageLoad
            )
          case Partnership => ???
          case unknownEntityType => throw new RuntimeException(s"Unknown organisation type $unknownEntityType")
        }
      case unknownManagementType => throw new RuntimeException(s"Unknown pension management type $unknownManagementType")
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
