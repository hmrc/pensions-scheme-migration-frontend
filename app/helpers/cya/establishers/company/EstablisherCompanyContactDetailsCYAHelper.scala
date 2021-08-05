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

package helpers.cya.establishers.company

import controllers.establishers.company.contact.routes
import helpers.cya.CYAHelper
import helpers.cya.CYAHelper.getCompanyName
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.contact.{EnterEmailId, EnterPhoneId}
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.MessageInterpolators
import uk.gov.hmrc.viewmodels.SummaryList.Row
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class EstablisherCompanyContactDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  def contactDetailsRows(
                          index: Index
                        )(
                          implicit request: DataRequest[AnyContent],
                          messages: Messages
                        ): Seq[Row] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val establisherName: String =
      getCompanyName(CompanyDetailsId(index))

    val rowsWithoutDynamicIndices = Seq(
      Some(answerOrAddRow(
        id = EnterEmailId(index),
        message = Message("messages__enterEmail", establisherName).resolve,
        url = Some(routes.EnterEmailController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__enterEmail__cya__visuallyHidden".withArgs(establisherName))
      )),
      Some(answerOrAddRow(
        id = EnterPhoneId(index),
        message = Message("messages__enterPhone", establisherName).resolve,
        url = Some(routes.EnterPhoneController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__enterPhone__cya__visuallyHidden".withArgs(establisherName))
      ))).flatten

    rowsWithDynamicIndices(rowsWithoutDynamicIndices)

  }
}
