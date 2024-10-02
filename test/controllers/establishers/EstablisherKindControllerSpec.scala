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

package controllers.establishers

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.establishers.EstablisherKindFormProvider
import identifiers.establishers.EstablisherKindId
import identifiers.establishers.individual.EstablisherNameId
import matchers.JsonMatchers
import models.establishers.EstablisherKind
import models.{Index, PersonName, Scheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.Application
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.{schemeName, ua}
import utils.{Enumerable, TwirlMigration, UserAnswers}
import views.html.establishers.EstablisherKindView

import scala.concurrent.Future
class EstablisherKindControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val index: Index = Index(0)
  private val kind: EstablisherKind = EstablisherKind.Individual
  private val userAnswers: Option[UserAnswers] = ua.set(EstablisherNameId(0), PersonName("Jane", "Doe")).toOption
  private val form: Form[EstablisherKind] = new EstablisherKindFormProvider()()

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = controllers.establishers.routes.EstablisherKindController.onPageLoad(index).url
  private def httpPathPOST: String = controllers.establishers.routes.EstablisherKindController.onSubmit(index).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq(kind.toString)
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }


  "EstablisherKindController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val request = httpGETRequest(httpPathGET)
      val result: Future[Result] = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[EstablisherKindView].apply(
        form,
        schemeName,
        TwirlMigration.toTwirlRadios(EstablisherKind.radios(form)),
        routes.EstablisherKindController.onSubmit(index)
      )(request, messages)

      compareResultAndView(result, view)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {

      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(EstablisherKindId(0)), any(), any())(any()))
        .thenReturn(controllers.establishers.individual.routes.EstablisherNameController.onPageLoad(0))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)

      redirectLocation(result) mustBe Some(controllers.establishers.individual.routes.EstablisherNameController.onPageLoad(0).url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }
}
