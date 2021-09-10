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

package controllers.establishers.partnership.partner.address

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.address.AddressYearsFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.address.AddressYearsId
import matchers.JsonMatchers
import models.{CheckMode, NormalMode, PersonName}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.Application
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.{schemeName, ua}
import utils.{Data, Enumerable, UserAnswers}

import scala.concurrent.Future

class AddressYearsControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val personName: PersonName = PersonName("Jane", "Doe")
  private val userAnswers: Option[UserAnswers] = Some(ua.setOrException(PartnerNameId(0,0), personName))
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()
  private val httpPathGET: String = controllers.establishers.partnership.partner.address.routes.AddressYearsController.onPageLoad(0, 0, NormalMode).url
  private val httpPathGETCheck: String = controllers.establishers.partnership.partner.address.routes.AddressYearsController.onPageLoad(0, 0, CheckMode).url
  private val httpPathPOST: String = controllers.establishers.partnership.partner.address.routes.AddressYearsController.onSubmit(0, 0, NormalMode).url
  private val form: Form[Boolean] = new AddressYearsFormProvider()("")

  private val jsonToPassToTemplate: Form[Boolean] => JsObject = form =>
    Json.obj(
      "form" -> form,
      "schemeName" -> schemeName,
      "radios" -> Radios.yesNo(form("value"))
    )

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "AddressYears Controller" must {

    "Return OK and the correct view for a GET" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, Data.schemeName)
        .setOrException(PartnerNameId(0, 0), personName)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result: Future[Result] = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      verify(mockRenderer, times(1))
        .render(ArgumentMatchers.eq("address/addressYears.njk"), jsonCaptor.capture())(any())

      (jsonCaptor.getValue \ "schemeName").toOption.map(_.as[String]) mustBe Some(Data.schemeName)
    }

    "return OK and the correct view for a GET when the question has previously been answered" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, Data.schemeName)
        .setOrException(PartnerNameId(0, 0), personName)
        .setOrException(AddressYearsId(0, 0), true)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result: Future[Result] = route(application, httpGETRequest(httpPathGETCheck)).value

      status(result) mustEqual OK

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      verify(mockRenderer, times(1))
        .render(ArgumentMatchers.eq("address/addressYears.njk"), jsonCaptor.capture())(any())

      jsonCaptor.getValue must containJson(jsonToPassToTemplate(form.fill(true)))
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      val ua: UserAnswers = UserAnswers()

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result: Future[Result] = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.IndexController.onPageLoad().url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {

      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(any(), any(), any())(any()))
        .thenReturn(controllers.establishers.partnership.partner.details.routes.CheckYourAnswersController.onPageLoad(0,0))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)

      redirectLocation(result) mustBe Some(controllers.establishers.partnership.partner.details.routes.CheckYourAnswersController.onPageLoad(0,0).url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect to Session Expired page for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.IndexController.onPageLoad().url
    }
  }

}

