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

package controllers.trustees

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.trustees.TrusteeKindFormProvider
import identifiers.trustees.TrusteeKindId
import identifiers.trustees.individual.TrusteeNameId
import matchers.JsonMatchers
import models.trustees.TrusteeKind
import models.{Index, PersonName, Scheme}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.{schemeName, ua}
import utils.{Enumerable, TwirlMigration, UserAnswers}

import scala.concurrent.Future
class TrusteeKindControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val index: Index = Index(0)
  private val kind: TrusteeKind = TrusteeKind.Individual
  private val userAnswers: Option[UserAnswers] = ua.set(TrusteeNameId(0), PersonName("Jane", "Doe")).toOption
  private val form: Form[TrusteeKind] = new TrusteeKindFormProvider()()

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = controllers.trustees.routes.TrusteeKindController.onPageLoad(index).url
  private def httpPathPOST: String = controllers.trustees.routes.TrusteeKindController.onSubmit(index).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq(kind.toString)
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
  }


  "TrusteeKindController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val req = httpGETRequest(httpPathGET)
      val result = route(app, req).value

      status(result) mustEqual OK
      compareResultAndView(result,
        app.injector.instanceOf[views.html.trustees.TrusteeKindView].apply(
          form,
          controllers.trustees.routes.TrusteeKindController.onSubmit(index),
          schemeName,
          TwirlMigration.toTwirlRadios(TrusteeKind.radios(form))
        )(req, implicitly)
      )
    }

    "redirect back to list of schemes for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "Redirect to next page when valid data is submitted" in {
      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(TrusteeKindId(0, TrusteeKind.Individual)), any(), any())(any()))
        .thenReturn(controllers.trustees.individual.routes.TrusteeNameController.onPageLoad(0))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) mustBe Some(controllers.trustees.individual.routes.TrusteeNameController.onPageLoad(0).url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }
}
