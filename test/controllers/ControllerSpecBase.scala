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


import base.SpecBase
import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import connectors.{EmailConnector, LegacySchemeDetailsConnector, MinimalDetailsConnector}
import controllers.actions._
import navigators.CompoundNavigator
import org.mockito.ArgumentMatchers.any
import org.mockito.{Mockito, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.Helpers.{GET, POST}
import play.api.test.{FakeHeaders, FakeRequest}
import services.DataUpdateService
import uk.gov.hmrc.nunjucks.NunjucksRenderer
import utils.{CountryOptions, Enumerable, FakeCountryOptions}

import scala.concurrent.ExecutionContext

trait ControllerSpecBase extends SpecBase with BeforeAndAfterEach  with Enumerable.Implicits with MockitoSugar {

  implicit val global: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val onwardCall: Call = Call("GET", "onwardCall")

  override def beforeEach(): Unit = {
    Mockito.reset(mockRenderer)
    Mockito.reset(mockUserAnswersCacheConnector)
    Mockito.reset(mockCompoundNavigator)
    when(mockCompoundNavigator.nextPage(any(), any(), any())(any()))
      .thenReturn(onwardCall)
  }


  protected val mockAppConfig: AppConfig = mock[AppConfig]

  protected val mockUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  protected val mockCompoundNavigator: CompoundNavigator = mock[CompoundNavigator]
  protected val mockRenderer: NunjucksRenderer = mock[NunjucksRenderer]
  protected val mockMinimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]
  protected val mockEmailConnector: EmailConnector = mock[EmailConnector]
  protected val mockLegacySchemeDetailsConnector: LegacySchemeDetailsConnector = mock[LegacySchemeDetailsConnector]
  protected val mockDataUpdateService: DataUpdateService = mock[DataUpdateService]

  def modules: Seq[GuiceableModule] = Seq(
    bind[AuthAction].to[FakeAuthAction],
    bind[DataRequiredAction].to[DataRequiredActionImpl],
    bind[NunjucksRenderer].toInstance(mockRenderer),
    bind[AppConfig].toInstance(mockAppConfig),
    bind[UserAnswersCacheConnector].toInstance(mockUserAnswersCacheConnector),
    bind[CompoundNavigator].toInstance(mockCompoundNavigator),
    bind[CountryOptions].toInstance(FakeCountryOptions.testData)
  )

  protected def applicationBuilderMutableRetrievalAction(
                                                          mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction,
                                                          extraModules: Seq[GuiceableModule] = Seq.empty
                                                        ): GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        //turn off metrics
        "metrics.jvm" -> false,
        "metrics.enabled" -> false
      )
      .overrides(
        modules ++ extraModules ++ Seq[GuiceableModule](
          bind[DataRetrievalAction].toInstance(mutableFakeDataRetrievalAction)
        ): _*
      )
  }

  protected def httpGETRequest(path: String): FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, path)

  protected def httpPOSTRequest(path: String, values: Map[String, Seq[String]]): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest
      .apply(
        method = POST,
        uri = path,
        headers = FakeHeaders(Seq(HeaderNames.HOST -> "localhost")),
        body = AnyContentAsFormUrlEncoded(values))

}
