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

package base

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import models.requests.DataRequest
import org.mockito.MockitoSugar.mock
import org.scalatest.Assertion
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.domain.PsaId
import utils.Data.{migrationLock, psaId}
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.language.implicitConversions

trait SpecBase
  extends PlaySpec
    with GuiceOneAppPerSuite {


  val onwardCall: Call = Call("GET", "onwardCall")

  protected val mockAppConfig: AppConfig = mock[AppConfig]
  protected val mockUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]

  implicit val global: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      //turn off metrics
      "auditing.enabled" -> false,
      "metrics.jvm"     -> false,
      "metrics.enabled" -> false
    )
    .build()

  def injector: Injector = app.injector

  def appConfig: AppConfig = injector.instanceOf[AppConfig]

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "/foo")

  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  implicit def fakeDataRequest(ua: UserAnswers = UserAnswers(Json.obj())): DataRequest[AnyContent] =
    DataRequest(
      request = fakeRequest,
      userAnswers = ua,
      psaId = PsaId(psaId),
      lock = migrationLock
    )

  implicit def messages: Messages = messagesApi.preferred(fakeRequest)

  def controllerComponents: MessagesControllerComponents = injector.instanceOf[MessagesControllerComponents]

  protected def compareResultAndView(
                                      result: Future[Result],
                                      view: Html
                                    ): Assertion = {
    org.scalatest.Assertions.assert(
      play.api.test.Helpers.contentAsString(result)(1.seconds).removeAllNonces().filterAndTrim
        == view.toString().filterAndTrim
    )
  }

  implicit class StringOps(s: String) {

    def filterAndTrim: String =
      s.split("\n")
        .filterNot(_.contains("csrfToken"))
        .map(_.trim)
        .mkString
    def removeAllNonces(): String = s.replaceAll("""nonce="[^"]*"""", "")
  }
}

object SpecBase extends SpecBase
