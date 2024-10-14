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

package controllers.trustees.company.details

import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.trustees.company.CompanyDetailsId
import matchers.JsonMatchers
import models.{CompanyDetails, Index, NormalMode}
import org.scalatest.TryValues
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data.ua
import utils.UserAnswers
import views.html.details.WhatYouWillNeedCompanyDetailsView

import scala.concurrent.Future

class WhatYouWillNeedControllerSpec extends ControllerSpecBase with JsonMatchers with TryValues {

  private val index: Index = Index(0)
  private val companyName: CompanyDetails = CompanyDetails("test company")
  private val userAnswers: UserAnswers = ua.set(CompanyDetailsId(0), companyName).success.value
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = routes.WhatYouWillNeedController.onPageLoad(index).url

  "WhatYouWillNeedController" must {
    "return OK and the correct view for a GET" in {

      mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))

      val request = httpGETRequest(httpPathGET)
      val result: Future[Result] = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[WhatYouWillNeedCompanyDetailsView].apply(
        Messages("messages__title_company"),
        companyName.companyName,
        routes.HaveCompanyNumberController.onPageLoad(index, NormalMode).url,
        "Test scheme name"
      )(request, messages)

      compareResultAndView(result, view)
    }
  }
}
