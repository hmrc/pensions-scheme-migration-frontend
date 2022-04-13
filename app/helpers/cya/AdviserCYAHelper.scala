/*
 * Copyright 2022 HM Revenue & Customs
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

package helpers.cya

import controllers.adviser.routes
import identifiers.adviser.{AddressId, AdviserNameId, EnterEmailId, EnterPhoneId}
import models.CheckMode
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.MessageInterpolators
import uk.gov.hmrc.viewmodels.SummaryList.Row
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class AdviserCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  //scalastyle:off method.length
  def detailsRows(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[Row] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val adviserName: String = ua.get(AdviserNameId).getOrElse(throw MandatoryAnswerMissingException(AdviserNameId.toString))

    val rowsWithoutDynamicIndices = Seq(
      Some(answerOrAddRow(
        id = AdviserNameId,
        message = Message("messages__adviser__name__cya").resolve,
        url = Some(routes.AdviserNameController.onPageLoad(CheckMode).url),
        visuallyHiddenText = Some(msg"messages__adviser__name__cya__visuallyHidden"),
        answerTransform = answerStringTransform
      )),
      Some(answerOrAddRow(
        id = EnterEmailId,
        message = Message("messages__enterEmail_cya_label", adviserName).resolve,
        url = Some(routes.EnterEmailController.onPageLoad(CheckMode).url),
        visuallyHiddenText = Some(msg"messages__enterEmail__cya__visuallyHidden".withArgs(adviserName))
      )),
      Some(answerOrAddRow(
        id = EnterPhoneId,
        message = Message("messages__enterPhone_cya_label", adviserName).resolve,
        url = Some(routes.EnterPhoneController.onPageLoad(CheckMode).url),
        visuallyHiddenText = Some(msg"messages__enterPhone__cya__visuallyHidden".withArgs(adviserName))
      )),
      Some(answerOrAddRow(
        id = AddressId,
        message = Message("addressList_cya_label", adviserName).resolve,
        url = Some(routes.EnterPostcodeController.onPageLoad(CheckMode).url),
        visuallyHiddenText = Some(msg"messages__visuallyHidden__address".withArgs(adviserName)), answerAddressTransform
      ))

    ).flatten
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}
