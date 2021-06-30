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
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TestMongoController @Inject()(
                                     appConfig: AppConfig,
                                     lockCacheConnector: LockCacheConnector,
                                     userAnswersCacheConnector: UserAnswersCacheConnector,
                                     mcc: MessagesControllerComponents,
                                     renderer: Renderer)
                                   (implicit val ec: ExecutionContext) extends FrontendController(mcc) {

  implicit val config: AppConfig = appConfig

  def setLock(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    val lock: MigrationLock = MigrationLock(pstr, "dummy cred", "A2100005")
    lockCacheConnector.setLock(lock).flatMap { response =>
      val json = Json.obj(
        "heading" -> "Set lock",
        "lock" -> lock.toString,
        "response" ->  Json.prettyPrint(response)
      )
      renderer.render("testMongo.njk", json).map(Ok(_))
    }
  }

  def getLock(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    val lock: MigrationLock = MigrationLock(pstr, "dummy cred", "A2100005")
    lockCacheConnector.getLock(lock).flatMap { response =>
      val json = Json.obj(
        "heading" -> "Get lock",
        "lock" -> lock.toString,
        "response" ->  response.toString
      )
      renderer.render("testMongo.njk", json).map(Ok(_))
    }
  }

  def getLockOnScheme(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    lockCacheConnector.getLockOnScheme(pstr).flatMap { response =>
      val json = Json.obj(
        "heading" -> "Get lock on scheme",
        "pstr" -> pstr,
        "response" ->  response.toString
      )
      renderer.render("testMongo.njk", json).map(Ok(_))
    }
  }

  def getLockByUser(): Action[AnyContent] = Action.async { implicit request =>

    lockCacheConnector.getLockByUser.flatMap { response =>
      val json = Json.obj(
        "heading" -> "Get lock by user",
        "response" ->  response.toString
      )
      renderer.render("testMongo.njk", json).map(Ok(_))
    }
  }

  def removeLock(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    val lock: MigrationLock = MigrationLock(pstr, "dummy cred", "A2100005")
    lockCacheConnector.removeLock(lock).flatMap { response =>
      val json = Json.obj(
        "heading" -> "Remove lock",
        "lock" -> lock.toString,
        "response" ->  response.toString
      )
      renderer.render("testMongo.njk", json).map(Ok(_))
    }
  }

  def removeLockOnScheme(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    lockCacheConnector.removeLockOnScheme(pstr).flatMap { response =>
      val json = Json.obj(
        "heading" -> "Remove lock on scheme ",
        "pstr" -> pstr,
        "response" ->  response.toString
      )
      renderer.render("testMongo.njk", json).map(Ok(_))
    }
  }

  def removeLockByUser(): Action[AnyContent] = Action.async { implicit request =>

    lockCacheConnector.removeLockByUser.flatMap { response =>
      val json = Json.obj(
        "heading" -> "Remove lock by user",
        "response" ->  response.toString
      )
      renderer.render("testMongo.njk", json).map(Ok(_))
    }
  }

  def save(pstr: String): Action[AnyContent] = Action.async { implicit request =>
    val lock: MigrationLock = MigrationLock(pstr, "dummy cred", "A2100005")

    val data: JsValue = Json.obj(
      "establishers" -> Json.arr(
        Json.obj("establisherDetails" -> Json.obj(
          "firstName" -> "other",
          "lastName" -> "xyz",
          "isDeleted" -> false
        ),
          "establisherKind" -> "individual",
          "isEstablisherNew" -> true,
        "phone" -> "88",
        "email" -> "s@s.com")),
      "schemeName" -> "Migration scheme",
      "schemeType" -> Json.obj(
        "name" -> "other",
        "schemeTypeDetails" -> "xyz"
      ),
      "schemeEstablishedCountry" -> "GB",
      "investmentRegulated" -> true,
      "occupationalPensionScheme" -> true,
    )

    userAnswersCacheConnector.save(lock, data).flatMap { response =>
      val json = Json.obj(
        "heading" -> "Save data",
        "lock" -> lock.toString,
        "response" -> Json.prettyPrint(response),
        "data" -> Json.prettyPrint(data)
      )
      renderer.render("testMongo.njk", json).map(Ok(_))
    }
  }

  def get(pstr: String): Action[AnyContent] = Action.async { implicit request =>

    userAnswersCacheConnector.fetch(pstr).flatMap { response =>
      val json = Json.obj(
        "heading" -> "Fetch data",
        "pstr" -> pstr,
        "response" ->  response.toString
      )
      renderer.render("testMongo.njk", json).map(Ok(_))
    }
  }

  def remove(pstr: String): Action[AnyContent] = Action.async { implicit request =>

    userAnswersCacheConnector.remove(pstr).flatMap { response =>
      val json = Json.obj(
        "heading" -> "Remove data",
        "pstr" -> pstr,
        "response" ->  response.toString
      )
      renderer.render("testMongo.njk", json).map(Ok(_))
    }
  }

}
