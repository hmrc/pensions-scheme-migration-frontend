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

package services.common.contact

import models.requests.DataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import viewmodels.Message

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CommonWhatYouWillNeedContactService @Inject()(
                                              val controllerComponents: MessagesControllerComponents,
                                              val renderer: Renderer,
                                              val messagesApi: MessagesApi
                                            ) extends NunjucksSupport
  with FrontendHeaderCarrierProvider
  with I18nSupport {
  private def viewTemplate = "whatYouWillNeedContact.njk"

  private case class TemplateData(
                                   name: String,
                                   pageHeading: String,
                                   entityType: String,
                                   continueUrl: String,
                                   schemeName: String
                                 )

  implicit private def templateDataWrites(implicit request: DataRequest[AnyContent]): OWrites[TemplateData] = Json.writes[TemplateData]

  def get(name: String,
          pageHeading: Message,
          entityType: Message,
          continueUrl: String,
          schemeName: String)(
           implicit request: DataRequest[AnyContent],
           ec: ExecutionContext): Future[Result] = {
    renderer.render(
      viewTemplate,
      getTemplateData(
        name, pageHeading.resolve, entityType.resolve, continueUrl, schemeName
      )
    ).map(Ok(_))
  }

  private def getTemplateData(name: String,
                              pageHeading: String,
                              entityType: String,
                              continueUrl: String,
                              schemeName: String): TemplateData = {
    TemplateData(
      name,
      pageHeading,
      entityType,
      continueUrl,
      schemeName
    )
  }
}
