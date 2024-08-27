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

import helpers.cya.CYAHelperForTwirl
import helpers.cya.CYAHelperForTwirl.getCompanyName
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.company.details._
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.viewmodels.MessageInterpolators
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class TrusteeCompanyDetailsCYAHelperForTwirl
  extends CYAHelperForTwirl
    with Enumerable.Implicits {

  def detailsRows(
                   index: Index
                 )(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[SummaryListRow] = {
    implicit val ua: UserAnswers = request.userAnswers

    val companyName: String = getCompanyName(CompanyDetailsId(index))

    rowsWithDynamicIndices(
      companyNumberAnswers(index, companyName) ++
      utrAnswers(index, companyName) ++
      vatAnswers(index, companyName) ++
      payeAnswers(index, companyName))
  }

  private def companyNumberAnswers(index: Index, companyName: String)(implicit ua: UserAnswers, messages: Messages): Seq[SummaryListRow] = {
    val message = Message("messages__haveCompanyNumber", companyName)
    val url = Some(controllers.trustees.company.details.routes.HaveCompanyNumberController.onPageLoad(index, CheckMode).url)
    val visuallyHidden = Some(msg"messages__haveCompanyNumber__cya__visuallyHidden".withArgs(companyName))

    ua.get(HaveCompanyNumberId(index)) match {

      case None => Seq(addRow(message, url, visuallyHidden))

      case Some(true) => Seq(
        answerRow(message, Message(booleanToText(true)), url, visuallyHidden),

        answerOrAddRow(
          id = CompanyNumberId(index),
          message = Message("messages__companyNumber__cya", companyName),
          url = Some(controllers.trustees.company.details.routes.CompanyNumberController.onPageLoad(index, CheckMode).url),
          visuallyHiddenText = Some(msg"messages__companyNumber__cya__visuallyHidden".withArgs(companyName)),
          answerTransform = referenceValueTransform
        ))
      case Some(false) => Seq(
        answerRow(message, Message(booleanToText(false)), url, visuallyHidden),
        answerOrAddRow(
          id = NoCompanyNumberReasonId(index),
          message = Message("messages__whyNoCompanyNumber", companyName),
          url = Some(controllers.trustees.company.details.routes.NoCompanyNumberReasonController.onPageLoad(index, CheckMode).url),
          visuallyHiddenText = Some(msg"messages__whyNoCompanyNumber__cya__visuallyHidden".withArgs(companyName))
        ))
    }
  }

  private def utrAnswers(index: Index, companyName: String)(implicit ua: UserAnswers, messages: Messages): Seq[SummaryListRow] = {
    val message = Message("messages__hasUTR", companyName)
    val url = Some(controllers.trustees.company.details.routes.HaveUTRController.onPageLoad(index, CheckMode).url)
    val visuallyHidden = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(companyName))

    ua.get(HaveUTRId(index)) match {
      case None => Seq(addRow(message, url, visuallyHidden))
      case Some(true) => Seq(
        answerRow(message, Message(booleanToText(true)), url, visuallyHidden),
        answerOrAddRow(
          id = CompanyUTRId(index),
          message = Message("messages__enterUTR__cya_label", companyName),
          url = Some(controllers.trustees.company.details.routes.UTRController.onPageLoad(index, CheckMode).url),
          visuallyHiddenText = Some(msg"messages__enterUTR__cya__visuallyHidden".withArgs(companyName)),
          answerTransform = referenceValueTransform
        ))
      case Some(false) => Seq(
        answerRow(message, Message(booleanToText(false)), url, visuallyHidden),
        answerOrAddRow(
          id = NoUTRReasonId(index),
          message = Message("messages__whyNoUTR", companyName),
          url = Some(controllers.trustees.company.details.routes.NoUTRReasonController.onPageLoad(index, CheckMode).url),
          visuallyHiddenText = Some(msg"messages__whyNoUTR__cya__visuallyHidden".withArgs(companyName))
        ))
    }
  }

  private def vatAnswers(index: Index, companyName: String)(implicit ua: UserAnswers, messages: Messages): Seq[SummaryListRow] = {
    val message = Message("messages__haveVAT", companyName)
    val url = Some(controllers.trustees.company.details.routes.HaveVATController.onPageLoad(index, CheckMode).url)
    val visuallyHidden = Some(msg"messages__haveVAT__cya__visuallyHidden".withArgs(companyName))

    ua.get(HaveVATId(index)) match {
      case None => Seq(addRow(message, url, visuallyHidden))

      case Some(true) => Seq(
        answerRow(message, Message(booleanToText(true)), url, visuallyHidden),
        answerOrAddRow(
          id = VATId(index),
          message = Message("messages__vat__cya", companyName),
          url = Some(controllers.trustees.company.details.routes.VATController.onPageLoad(index, CheckMode).url),
          visuallyHiddenText = Some(msg"messages__vat__cya__visuallyHidden".withArgs(companyName)),
          answerTransform = referenceValueTransform
        ))

      case Some(false) => Seq(answerRow(message, Message(booleanToText(false)), url, visuallyHidden))
    }
  }

  private def payeAnswers(index: Index, companyName: String)(implicit ua: UserAnswers, messages: Messages): Seq[SummaryListRow] = {
    val message = Message("messages__havePAYE", companyName)
    val url = Some(controllers.trustees.company.details.routes.HavePAYEController.onPageLoad(index, CheckMode).url)
    val visuallyHidden = Some(msg"messages__havePAYE__cya__visuallyHidden".withArgs(companyName))

    ua.get(HavePAYEId(index)) match {
      case None => Seq(addRow(message, url, visuallyHidden))

      case Some(true) => Seq(
        answerRow(message, Message(booleanToText(true)), url, visuallyHidden),
        answerOrAddRow(
          id = PAYEId(index),
          message = Message("messages__paye_cya", companyName),
          url = Some(controllers.trustees.company.details.routes.PAYEController.onPageLoad(index, CheckMode).url),
          visuallyHiddenText = Some(msg"messages__paye__cya__visuallyHidden".withArgs(companyName)),
          answerTransform = referenceValueTransform
        ))

      case Some(false) => Seq(answerRow(message, Message(booleanToText(false)), url, visuallyHidden))
    }
  }

}
