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

package helpers.cya.establishers.individual

import helpers.cya.CYAHelper
import helpers.cya.CYAHelper.getName
import helpers.routes.EstablishersIndividualRoutes
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address.{AddressYearsId, PreviousAddressId, AddressId}
import models.{Index, NormalMode}
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.MessageInterpolators
import uk.gov.hmrc.viewmodels.SummaryList.Row
import utils.{UserAnswers, Enumerable}
import viewmodels.Message

class EstablisherAddressCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  //scalastyle:off method.length
  def rows(index: Index)(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[Row] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val establisherName: String =
      getName(EstablisherNameId(index))

    val seqRowAddressAndYears = Seq(
      answerOrAddRow(
        AddressId(index),
        Message("messages__establisherAddress__whatYouWillNeed_h1", establisherName).resolve,
        Some(controllers.establishers.individual.address.routes.EnterPostcodeController.onPageLoad(index).url),
        Some(msg"messages__visuallyHidden__address".withArgs(establisherName)), answerAddressTransform
      ),
      answerOrAddRow(
        AddressYearsId(index),
        Message("addressYears.title", establisherName).resolve,
        Some(EstablishersIndividualRoutes.timeAtAddressRoute(index, NormalMode).url),
        Some(msg"messages__visuallyhidden__addressYears".withArgs(establisherName)), answerBooleanTransform
      )
    )

    val seqRowPreviousAddress = if (ua.get(AddressYearsId(index)).getOrElse(true).equals(true)) {
      Nil
    } else {
      Seq(
        answerOrAddRow(
          PreviousAddressId(index),
          Message("messages__establisherPreviousAddress").resolve,
          Some(EstablishersIndividualRoutes.enterPreviousPostcodeRoute(index, NormalMode).url),
          Some(msg"messages__visuallyHidden__previousAddress".withArgs(establisherName)), answerAddressTransform
        )
      )
    }

    val rowsWithoutDynamicIndices = seqRowAddressAndYears ++ seqRowPreviousAddress
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}
