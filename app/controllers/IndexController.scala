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

package controllers

import config.AppConfig
import play.api.mvc._
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.TwirlMigration
import views.html.IndexView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class IndexController @Inject()(
                                      appConfig: AppConfig,
                                      mcc: MessagesControllerComponents,
                                      renderer: Renderer,
                                      indexView: IndexView)
                                    (implicit val ec: ExecutionContext) extends FrontendController(mcc) {

  implicit val config: AppConfig = appConfig

  val onPageLoad: Action[AnyContent] = Action.async { implicit request =>
    TwirlMigration.duoTemplate(renderer.render("index.njk"), indexView()).map { Ok(_) }
  }

}
