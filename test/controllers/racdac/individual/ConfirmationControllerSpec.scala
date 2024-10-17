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

package controllers.racdac.individual

import connectors.cache.{CurrentPstrCacheConnector, LockCacheConnector}
import connectors.{ListOfSchemesConnector, MinimalDetailsConnector}
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import identifiers.beforeYouStart.SchemeNameId
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import utils.Data.ua
import utils.{Data, Enumerable, UserAnswers}

import scala.concurrent.Future
class ConfirmationControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val userAnswers: Option[UserAnswers] = ua.set(SchemeNameId, Data.schemeName).toOption
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockCurrentPstrCacheConnector: CurrentPstrCacheConnector = mock[CurrentPstrCacheConnector]
  private val mockLockCacheConnector: LockCacheConnector = mock[LockCacheConnector]
  private val mockListOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val extraModules: Seq[GuiceableModule] = Seq(
    bind[MinimalDetailsConnector].to(mockMinimalDetailsConnector),
    bind[CurrentPstrCacheConnector].to(mockCurrentPstrCacheConnector),
    bind[ListOfSchemesConnector].to(mockListOfSchemesConnector),
    bind[LockCacheConnector].to(mockLockCacheConnector)
  )
  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.racdac.individual.routes.ConfirmationController.onPageLoad.url

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockUserAnswersCacheConnector.remove(any())(any(), any())).thenReturn(Future.successful(Ok))
    when(mockCurrentPstrCacheConnector.remove(any(), any())).thenReturn(Future.successful(Ok))
    when(mockLockCacheConnector.removeLock(any())(any(), any())).thenReturn(Future.successful(Ok))
    when(mockListOfSchemesConnector.removeCache(any())(any(), any())).thenReturn(Future.successful(Ok))
  }

  "ConfirmationController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      when(mockMinimalDetailsConnector.getPSAEmail(any(), any())).thenReturn(Future.successful(Data.email))
      val req = httpGETRequest(httpPathGET)
      val result = route(app, req).value

      status(result) mustEqual OK

      compareResultAndView(result,
        app.injector.instanceOf[views.html.racdac.individual.ConfirmationView].apply(
          "pstr",
          Data.schemeName,
          Data.email,
          mockAppConfig.yourPensionSchemesUrl,
          mockAppConfig.psaOverviewUrl
        )(req, implicitly)
      )
    }

  }
}
