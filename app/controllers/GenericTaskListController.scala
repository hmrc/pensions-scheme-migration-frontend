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

package controllers

import models.EntitySpoke
import models.requests.DataRequest
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

trait GenericTaskListController
  extends FrontendBaseController
    with I18nSupport
    with Retrievals
    with NunjucksSupport {
  protected implicit def executionContext: ExecutionContext

  protected val renderer: Renderer

  def get(
           spokes: Seq[EntitySpoke],
           entityName: String,
           schemeName: String,
           entityType: String,
           submitUrl: String
         )(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val totalSpokes = spokes.size
    val completedCount = spokes.count(_.isCompleted.contains(true))

    renderer.render(
      template = "spokeTaskList.njk",
      ctx = Json.obj(
        "taskSections" -> spokes,
        "entityName" -> entityName,
        "schemeName" -> schemeName,
        "totalSpokes" -> totalSpokes,
        "completedCount" -> completedCount,
        "entityType" -> entityType,
        "submitUrl" -> submitUrl
      )
    ).map(Ok(_))
  }
}
