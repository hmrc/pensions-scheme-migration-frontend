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



import connectors.cache.{CurrentPstrCacheConnector, LockCacheConnector}
import connectors.{ListOfSchemesConnector, MinimalDetailsConnector}
import controllers.actions.MutableFakeDataRetrievalAction
import identifiers.establishers.individual.EstablisherNameId
import matchers.JsonMatchers
import models.PersonName
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import utils.Data.ua
import utils.{Data, Enumerable, UserAnswers}
import views.html.SchemeSuccessView

import scala.concurrent.Future

class SchemeSuccessControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val name = PersonName("Jane", "Doe")
  private val userAnswers: Option[UserAnswers] = ua.set(EstablisherNameId(0), name).toOption
  private val mockCurrentPstrCacheConnector: CurrentPstrCacheConnector = mock[CurrentPstrCacheConnector]
  private val mockLockCacheConnector: LockCacheConnector = mock[LockCacheConnector]
  private val mockListOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[MinimalDetailsConnector].to(mockMinimalDetailsConnector),
    bind[CurrentPstrCacheConnector].to(mockCurrentPstrCacheConnector),
    bind[ListOfSchemesConnector].to(mockListOfSchemesConnector),
    bind[LockCacheConnector].to(mockLockCacheConnector)
  )
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.routes.SchemeSuccessController.onPageLoad.url
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, httpPathGET)

  private val schemeName = Data.schemeName
  private val pstr = "pstr"
  private val email = Data.email
  private val yourSchemesLink = mockAppConfig.yourPensionSchemesUrl
  private val returnUrl = mockAppConfig.psaOverviewUrl

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(mockUserAnswersCacheConnector.remove(any())(any(), any())).thenReturn(Future.successful(Ok))
    when(mockCurrentPstrCacheConnector.remove(any(), any())).thenReturn(Future.successful(Ok))
    when(mockLockCacheConnector.removeLock(any())(any(), any())).thenReturn(Future.successful(Ok))
    when(mockListOfSchemesConnector.removeCache(any())(any(), any())).thenReturn(Future.successful(Ok))
  }


  "SchemeSuccessController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      when(mockMinimalDetailsConnector.getPSAEmail(any(), any())).thenReturn(Future.successful(Data.email))

      val view = application.injector.instanceOf[SchemeSuccessView].apply(
        schemeName, pstr, email, yourSchemesLink, returnUrl
      )(request, messages)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      compareResultAndView(result, view)
    }

  }
}
