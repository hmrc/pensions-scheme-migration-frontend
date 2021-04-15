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

package controllers.testonly

import config.AppConfig
import connectors.cache.{LockCacheConnector, UserAnswersCacheConnector}
import models.MigrationLock
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.TestMongoPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TestMongoController @Inject()(
                                     appConfig: AppConfig,
                                     lockCacheConnector: LockCacheConnector,
                                     userAnswersCacheConnector: UserAnswersCacheConnector,
                                     mcc: MessagesControllerComponents,
                                     testMongoPage: TestMongoPage)
                                   (implicit val ec: ExecutionContext) extends FrontendController(mcc) {

  implicit val config: AppConfig = appConfig

  def setLock(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    val lock: MigrationLock = MigrationLock(pstr, "dummy cred", "A2100005")
    lockCacheConnector.setLock(lock).map { response =>
      Ok(testMongoPage(s"set lock with details: $lock returned $response"))
    }
  }

  def getLock(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    val lock: MigrationLock = MigrationLock(pstr, "dummy cred", "A2100005")
    lockCacheConnector.getLock(lock).map { response =>
      Ok(testMongoPage(s"get lock with details: $lock returned $response"))
    }
  }

  def getLockOnScheme(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    lockCacheConnector.getLockOnScheme(pstr).map { response =>
      Ok(testMongoPage(s"get lock on scheme with details: $pstr returned ${response.toString}"))
    }
  }

  def getLockByUser(): Action[AnyContent] = Action.async { implicit request =>

    lockCacheConnector.getLockByUser.map { response =>
      Ok(testMongoPage(s"get lock by user returned $response"))
    }
  }

  def removeLock(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    val lock: MigrationLock = MigrationLock(pstr, "dummy cred", "A2100005")
    lockCacheConnector.removeLock(lock).map { response =>
      Ok(testMongoPage(s"remove lock with details: $lock returned $response"))
    }
  }

  def removeLockOnScheme(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    lockCacheConnector.removeLockOnScheme(pstr).map { response =>
      Ok(testMongoPage(s"remove lock on scheme with details: $pstr returned ${response.toString}"))
    }
  }

  def removeLockByUser(): Action[AnyContent] = Action.async { implicit request =>

    lockCacheConnector.removeLockByUser.map { response =>
      Ok(testMongoPage(s"remove lock by user returned $response"))
    }
  }

  def save(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    val lock: MigrationLock = MigrationLock(pstr, "dummy cred", "A2100005")
    val json: JsValue = Json.obj("test-key" -> "test-value")
    userAnswersCacheConnector.save(lock, json).map { response =>
      Ok(testMongoPage(s"save data $json with lock $lock returned $response"))
    }
  }

  def get(pstr: String): Action[AnyContent] = Action.async { implicit request =>

    userAnswersCacheConnector.fetch(pstr).map { response =>
      Ok(testMongoPage(s"fetch data with $pstr returned $response"))
    }
  }

  def remove(pstr: String): Action[AnyContent] = Action.async { implicit request =>

    userAnswersCacheConnector.remove(pstr).map { response =>
      Ok(testMongoPage(s"remove data with $pstr returned $response"))
    }
  }

}
