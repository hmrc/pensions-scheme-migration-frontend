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

package controllers.establishers.partnership.partner

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import identifiers.establishers.partnership.partner.PartnerNameId
import matchers.JsonMatchers
import models.{Index, NormalMode, PersonName}
import play.api.Application
import play.api.test.Helpers._
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}
import views.html.AlreadyDeletedView

class AlreadyDeletedControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val index: Index = Index(0)
  private val partnerIndex: Index = Index(0)
  private val name = PersonName("Jane", "Doe")
  private val userAnswers: Option[UserAnswers] = ua.set(PartnerNameId(0,0), name).toOption

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = controllers.establishers.partnership.partner.routes.AlreadyDeletedController.onPageLoad(index,partnerIndex).url

  private val request = httpGETRequest(httpPathGET)

  override def beforeEach(): Unit = {
    super.beforeEach()

  }


  "AlreadyDeletedController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[AlreadyDeletedView].apply(
        messages("messages__alreadyDeleted__partner_title"),
        name.fullName,
        Some(schemeName),
        controllers.establishers.partnership.routes.AddPartnersController.onPageLoad(0,NormalMode).url
      )(request, messages)

      compareResultAndView(result, view)
    }

  }
}
