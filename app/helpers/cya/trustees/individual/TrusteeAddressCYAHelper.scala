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

package helpers.cya.trustees.individual

import helpers.cya.CYAHelper
import helpers.cya.CYAHelper.getName
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address.{AddressId, AddressYearsId, PreviousAddressId}
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.{Enumerable, UserAnswers}

class TrusteeAddressCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  //scalastyle:off method.length
  def rows(index: Index)(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[SummaryListRow] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val trusteeName: String =
      getName(TrusteeNameId(index))

    val seqRowAddressAndYears = Seq(
      answerOrAddRow(
        AddressId(index),
        Messages("messages__address__whatYouWillNeed_h1", trusteeName),
        Some(controllers.trustees.individual.address.routes.EnterPostcodeController.onPageLoad(index, CheckMode).url),
        Some(Text(Messages("messages__visuallyHidden__address", trusteeName))), answerAddressTransform
      ),
      answerOrAddRow(
        AddressYearsId(index),
        Messages("addressYears.title", trusteeName),
        Some(controllers.trustees.individual.address.routes.AddressYearsController.onPageLoad(index, CheckMode).url),
        Some(Text(Messages("messages__visuallyhidden__trusteeAddressYears", trusteeName))), answerBooleanTransform
      )
    )

    val seqRowPreviousAddress = if (ua.get(AddressYearsId(index)).contains(true)) {
      Nil
    } else {
      Seq(
        answerOrAddRow(
          PreviousAddressId(index),
          Messages("messages__previousAddress", trusteeName),
          Some(controllers.trustees.individual.address.routes.EnterPreviousPostcodeController.onPageLoad(index, CheckMode).url),
          Some(Text(Messages("messages__visuallyHidden__previousAddress", trusteeName))), answerAddressTransform
        )
      )
    }

    val rowsWithoutDynamicIndices = seqRowAddressAndYears ++ seqRowPreviousAddress
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}
