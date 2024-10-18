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

package helpers.cya.establishers.partnership

import helpers.cya.CYAHelper
import helpers.cya.CYAHelper.getPartnershipName
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.establishers.partnership.address.{AddressId, AddressYearsId, PreviousAddressId, TradingTimeId}
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryListRow, Text}
import utils.{Enumerable, UserAnswers}

class EstablisherPartnershipAddressCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  //scalastyle:off method.length
  def rows(index: Index)(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[SummaryListRow] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val establisherName: String =
    getPartnershipName(PartnershipDetailsId(index))

    val seqRowAddressAndYears = Seq(
      answerOrAddRow(
        AddressId(index),
        Messages("messages__address__whatYouWillNeed_h1", establisherName),
        Some(controllers.establishers.partnership.address.routes.EnterPostcodeController.onPageLoad(index,CheckMode).url),
        Some(Text(Messages("messages__visuallyHidden__address", establisherName))), answerAddressTransform
      ),
      answerOrAddRow(
        AddressYearsId(index),
        Messages("addressYears.title", establisherName),
        Some(controllers.establishers.partnership.address.routes.AddressYearsController.onPageLoad(index,CheckMode).url),
        Some(Text(Messages("messages__visuallyhidden__addressYears", establisherName))), answerBooleanTransform
      )
    )
    val seqTradingTime = if (ua.get(AddressYearsId(index)).contains(false)) {
      Seq(
        answerOrAddRow(
          TradingTimeId(index),
          Messages("tradingTime.title", establisherName),
          Some(controllers.establishers.partnership.address.routes.TradingTimeController.onPageLoad(index,CheckMode).url),
          Some(Text(Messages("messages__visuallyhidden__TradingTime", establisherName))), answerBooleanTransform
        )
      )
    } else {
      Nil
    }

    val seqRowPreviousAddress =(ua.get(AddressYearsId(index)), ua.get(TradingTimeId(index))) match {
      case (Some(false), Some(true)) =>
        Seq(
          answerOrAddRow(
            PreviousAddressId(index),
            Messages("messages__previousAddress", establisherName),
            Some(controllers.establishers.partnership.address.routes.EnterPreviousPostcodeController.onPageLoad(index,CheckMode).url),
            Some(Text(Messages("messages__visuallyHidden__previousAddress", establisherName))), answerAddressTransform
          )
        )
      case _ => Nil
    }

    val rowsWithoutDynamicIndices = seqRowAddressAndYears ++ seqTradingTime ++ seqRowPreviousAddress
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}

