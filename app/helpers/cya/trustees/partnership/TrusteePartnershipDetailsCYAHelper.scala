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

package helpers.cya.trustees.partnership

import controllers.trustees.partnership.details.routes
import helpers.cya.CYAHelper
import helpers.cya.CYAHelper.getPartnershipName
import identifiers.trustees.partnership.PartnershipDetailsId
import identifiers.trustees.partnership.details._
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.MessageInterpolators
import uk.gov.hmrc.viewmodels.SummaryList.Row
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class TrusteePartnershipDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  def detailsRows(
                   index: Index
                 )(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[Row] = {
    implicit val ua: UserAnswers = request.userAnswers

    val partnershipName: String = getPartnershipName(PartnershipDetailsId(index))

    rowsWithDynamicIndices(
      utrAnswers(index, partnershipName) ++
        vatAnswers(index, partnershipName) ++
        payeAnswers(index, partnershipName))


  }


  private def utrAnswers(index: Index, partnershipName: String)(implicit ua: UserAnswers, messages: Messages): Seq[Row] = {
    val message = Message("messages__hasUTR", partnershipName)
    val url = Some(routes.HaveUTRController.onPageLoad(index, CheckMode).url)
    val visuallyHidden = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(partnershipName))

    ua.get(HaveUTRId(index)) match {
      case None => Seq(addRow(message, url, visuallyHidden))
      case Some(true) => Seq(
        answerRow(message, Message(booleanToText(true)), url, visuallyHidden),
        answerOrAddRow(
          id = PartnershipUTRId(index),
          message = Message("messages__enterUTR__cya_label", partnershipName),
          url = Some(routes.UTRController.onPageLoad(index, CheckMode).url),
          visuallyHiddenText = Some(msg"messages__enterUTR__cya__visuallyHidden".withArgs(partnershipName)),
          answerTransform = referenceValueTransform
        ))
      case Some(false) => Seq(
        answerRow(message, Message(booleanToText(false)), url, visuallyHidden),
        answerOrAddRow(
          id = NoUTRReasonId(index),
          message = Message("messages__whyNoUTR", partnershipName),
          url = Some(routes.NoUTRReasonController.onPageLoad(index, CheckMode).url),
          visuallyHiddenText = Some(msg"messages__whyNoUTR__cya__visuallyHidden".withArgs(partnershipName))
        ))
    }
  }

  private def vatAnswers(index: Index, partnershipName: String)(implicit ua: UserAnswers, messages: Messages): Seq[Row] = {
    val message = Message("messages__haveVAT", partnershipName)
    val url = Some(routes.HaveVATController.onPageLoad(index, CheckMode).url)
    val visuallyHidden = Some(msg"messages__haveVAT__cya__visuallyHidden".withArgs(partnershipName))

    ua.get(HaveVATId(index)) match {
      case None => Seq(addRow(message, url, visuallyHidden))

      case Some(true) => Seq(
        answerRow(message, Message(booleanToText(true)), url, visuallyHidden),
        answerOrAddRow(
          id = VATId(index),
          message = Message("messages__vat__cya", partnershipName),
          url = Some(routes.VATController.onPageLoad(index, CheckMode).url),
          visuallyHiddenText = Some(msg"messages__vat__cya__visuallyHidden".withArgs(partnershipName)),
          answerTransform = referenceValueTransform
        ))

      case Some(false) => Seq(answerRow(message, Message(booleanToText(false)), url, visuallyHidden))
    }
  }

  private def payeAnswers(index: Index, partnershipName: String)(implicit ua: UserAnswers, messages: Messages): Seq[Row] = {
    val message = Message("messages__havePAYE", partnershipName)
    val url = Some(routes.HavePAYEController.onPageLoad(index, CheckMode).url)
    val visuallyHidden = Some(msg"messages__havePAYE__cya__visuallyHidden".withArgs(partnershipName))

    ua.get(HavePAYEId(index)) match {
      case None => Seq(addRow(message, url, visuallyHidden))

      case Some(true) => Seq(
        answerRow(message, Message(booleanToText(true)), url, visuallyHidden),
        answerOrAddRow(
          id = PAYEId(index),
          message = Message("messages__paye_cya", partnershipName),
          url = Some(routes.PAYEController.onPageLoad(index, CheckMode).url),
          visuallyHiddenText = Some(msg"messages__paye__cya__visuallyHidden".withArgs(partnershipName)),
          answerTransform = referenceValueTransform
        ))

      case Some(false) => Seq(answerRow(message, Message(booleanToText(false)), url, visuallyHidden))
    }
  }

}
