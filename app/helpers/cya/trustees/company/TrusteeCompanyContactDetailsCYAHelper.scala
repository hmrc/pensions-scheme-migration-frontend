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

package helpers.cya.trustees.company

import controllers.trustees.company.contacts.routes
import helpers.cya.CYAHelper
import helpers.cya.CYAHelper.getCompanyName
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.company.contacts.{EnterEmailId, EnterPhoneId}
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class TrusteeCompanyContactDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  def contactDetailsRows(
                          index: Index
                        )(
                          implicit request: DataRequest[AnyContent],
                          messages: Messages
                        ): Seq[SummaryListRow] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val trusteeName: String =
      getCompanyName(CompanyDetailsId(index))

    val rowsWithoutDynamicIndices = Seq(
      Some(answerOrAddRow(
        id = EnterEmailId(index),
        message = Message("messages__enterEmail_cya_label", trusteeName).resolve,
        url = Some(routes.EnterEmailController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__enterEmail__cya__visuallyHidden", trusteeName)))
      )),
      Some(answerOrAddRow(
        id = EnterPhoneId(index),
        message = Message("messages__enterPhone_cya_label", trusteeName).resolve,
        url = Some(routes.EnterPhoneController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__enterPhone__cya__visuallyHidden", trusteeName)))
      ))).flatten

    rowsWithDynamicIndices(rowsWithoutDynamicIndices)

  }
}
