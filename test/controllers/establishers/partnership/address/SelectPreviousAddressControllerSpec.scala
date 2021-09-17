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

package controllers.establishers.partnership.address

import connectors.AddressLookupConnector
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import identifiers.establishers.partnership.address.EnterPreviousPostCodeId
import matchers.JsonMatchers
import models.TolerantAddress
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.{Data, Enumerable, UserAnswers}

import scala.concurrent.Future

class SelectPreviousAddressControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val mockAddressLookupConnector = mock[AddressLookupConnector]

  val extraModules: Seq[GuiceableModule] = Seq(
    bind[AddressLookupConnector].toInstance(mockAddressLookupConnector)
  )

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()
  private val httpPathGET: String = controllers.establishers.partnership.address.routes.SelectPreviousAddressController.onPageLoad(0).url
  private val httpPathPOST: String = controllers.establishers.partnership.address.routes.SelectPreviousAddressController.onSubmit(0).url

  private val seqAddresses = Seq(
    TolerantAddress(Some("1"),Some("1"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
    TolerantAddress(Some("2"),Some("2"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
  )

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("1")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "SelectPreviousAddress Controller" must {

    "Return OK and the correct view for a GET" in {
      val ua: UserAnswers = Data.ua
        .setOrException(EnterPreviousPostCodeId(0), seqAddresses)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result: Future[Result] = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      verify(mockRenderer, times(1))
        .render(ArgumentMatchers.eq("address/addressList.njk"), jsonCaptor.capture())(any())

      (jsonCaptor.getValue \ "schemeName").toOption.map(_.as[String]) mustBe Some(Data.schemeName)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      val ua: UserAnswers = UserAnswers()

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result: Future[Result] = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.IndexController.onPageLoad().url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {
      val ua: UserAnswers = Data.ua
        .setOrException(EnterPreviousPostCodeId(0), seqAddresses)

      when(mockCompoundNavigator.nextPage(any(), any(), any())(any()))
        .thenReturn(routes.CheckYourAnswersController.onPageLoad(0))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.onPageLoad(0).url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      val ua: UserAnswers = Data.ua
        .setOrException(EnterPreviousPostCodeId(0), seqAddresses)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST
    }

    "redirect to Session Expired page for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.IndexController.onPageLoad().url
    }
  }

}
