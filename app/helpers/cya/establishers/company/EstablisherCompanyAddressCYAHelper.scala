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

package helpers.cya.establishers.company

import helpers.cya.CYAHelperForTwirl
import helpers.cya.CYAHelperForTwirl.getCompanyName
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.address.{AddressId, AddressYearsId, PreviousAddressId, TradingTimeId}
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
//import uk.gov.hmrc.viewmodels.MessageInterpolators
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class EstablisherCompanyAddressCYAHelper
  extends CYAHelperForTwirl
    with Enumerable.Implicits {

  //scalastyle:off method.length
  def rows(index: Index)(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[SummaryListRow] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val establisherName: String =
      getCompanyName(CompanyDetailsId(index))

    val seqRowAddressAndYears = Seq(
      answerOrAddRow(
        AddressId(index),
        Message("messages__address__whatYouWillNeed_h1", establisherName).resolve,
        Some(controllers.establishers.company.address.routes.EnterPostcodeController.onPageLoad(index,CheckMode).url),
        Some(Message("messages__visuallyHidden__address", establisherName)), answerAddressTransform
      ),
      answerOrAddRow(
        AddressYearsId(index),
        Message("addressYears.title", establisherName).resolve,
        Some(controllers.establishers.company.address.routes.AddressYearsController.onPageLoad(index,CheckMode).url),
        Some(Message("messages__visuallyhidden__addressYears", establisherName)), answerBooleanTransform()
      )
    )

    val seqTradingTime = if (ua.get(AddressYearsId(index)).contains(false)) {
      Seq(
        answerOrAddRow(
          TradingTimeId(index),
          Message("tradingTime.title", establisherName).resolve,
          Some(controllers.establishers.company.address.routes.TradingTimeController.onPageLoad(index,CheckMode).url),
          Some(Message("messages__visuallyhidden__TradingTime", establisherName)), answerBooleanTransform()
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
            Message("messages__previousAddress", establisherName).resolve,
            Some(controllers.establishers.company.address.routes.EnterPreviousPostcodeController.onPageLoad(index,CheckMode).url),
            Some(Message("messages__visuallyHidden__previousAddress", establisherName)), answerAddressTransform
          )
        )
      case _ => Nil
    }

    val rowsWithoutDynamicIndices = seqRowAddressAndYears ++ seqTradingTime ++ seqRowPreviousAddress
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}
