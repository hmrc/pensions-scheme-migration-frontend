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

package base

import config.AppConfig
import controllers.actions._
import models.requests.DataRequest
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice._
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.inject.{Injector, bind}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsEmpty, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.{Application, Environment}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.domain.PsaId
import utils.Data.{migrationLock, psaId}
import utils.UserAnswers

import scala.language.implicitConversions

trait SpecBase
  extends PlaySpec
    with GuiceOneAppPerSuite {

  def modules(mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction): Seq[GuiceableModule] =
    Seq(
      bind[AuthAction].toInstance(FakeAuthAction),
      bind[DataRetrievalAction].toInstance(mutableFakeDataRetrievalAction)
    )

  def applicationBuilder(
                          mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction,
                          extraModules: Seq[GuiceableModule] = Seq.empty
                        ): GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        "auditing.enabled" -> false,
        "metrics.enabled" -> false
      )
      .overrides(
        extraModules ++ modules(mutableFakeDataRetrievalAction): _*
      )
  }

  def appRunning(): Application = applicationBuilder().build()

  def injector: Injector = appRunning().injector

  protected def crypto: ApplicationCrypto = injector.instanceOf[ApplicationCrypto]

  def appConfig: AppConfig = injector.instanceOf[AppConfig]

  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "/foo")

  implicit def fakeDataRequest(ua: UserAnswers = UserAnswers(Json.obj())): DataRequest[AnyContent] =
    DataRequest(
      request = fakeRequest,
      userAnswers = ua,
      psaId = PsaId(psaId),
      lock = migrationLock
    )

  implicit def messages: Messages = messagesApi.preferred(fakeRequest)

  def environment: Environment = injector.instanceOf[Environment]

  def controllerComponents: MessagesControllerComponents = injector.instanceOf[MessagesControllerComponents]

  def assertNotRenderedById(doc: Document, id: String): Assertion = {
    assert(doc.getElementById(id) == null, "\n\nElement " + id + " was rendered on the page.\n")
  }

  def assertRenderedById(doc: Document, id: String): Assertion =
    assert(doc.getElementById(id) != null, "\n\nElement " + id + " was not rendered on the page.\n")
}

object SpecBase extends SpecBase
