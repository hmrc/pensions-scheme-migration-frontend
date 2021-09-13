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

package helpers.cya.trustees.individual

import controllers.trustees.individual.details.routes
import helpers.cya.CYAHelper
import helpers.cya.CYAHelper.getName
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.details._
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.MessageInterpolators
import uk.gov.hmrc.viewmodels.SummaryList.Row
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class TrusteeDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  def detailsRows(index: Index)
                 (implicit request: DataRequest[AnyContent],
                   messages: Messages): Seq[Row] = {
    implicit val ua: UserAnswers = request.userAnswers
    val trusteeName: String = getName(TrusteeNameId(index))

    val rowsWithoutDynamicIndices = Seq(
      Some(answerOrAddRow(
        id                 = TrusteeDOBId(index),
        message            = Message("messages__dob__h1", trusteeName).resolve,
        url                = Some(routes.TrusteeDOBController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__dob__cya__visuallyHidden".withArgs(trusteeName)),
        answerTransform = answerDateTransform
      )),
      Some(answerOrAddRow(
        id                 = TrusteeHasNINOId(index),
        message            = Message("messages__hasNINO", trusteeName).resolve,
        url                = Some(routes.TrusteeHasNINOController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(trusteeName)),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(TrusteeNINOId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = TrusteeNINOId(index),
            message            = Message("messages__enterNINO__cya", trusteeName),
            url                = Some(routes.TrusteeEnterNINOController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__hasNINO__cya__visuallyHidden".withArgs(trusteeName)),
            answerTransform    = referenceValueTransform
          )
      },
      ua.get(TrusteeNoNINOReasonId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = TrusteeNoNINOReasonId(index),
            message            = Message("messages__whyNoNINO", trusteeName),
            url                = Some(routes.TrusteeNoNINOReasonController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__whyNoNINO__cya__visuallyHidden".withArgs(trusteeName))
          )
      },
      Some(answerOrAddRow(
        id                 = TrusteeHasUTRId(index),
        message            = Message("messages__hasUTR", trusteeName).resolve,
        url                = Some(routes.TrusteeHasUTRController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(trusteeName)),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(TrusteeUTRId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = TrusteeUTRId(index),
            message            = Message("messages__enterUTR__cya_label", trusteeName),
            url                = Some(routes.TrusteeEnterUTRController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(trusteeName)),
            answerTransform    = referenceValueTransform
          )
      },
      ua.get(TrusteeNoUTRReasonId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = TrusteeNoUTRReasonId(index),
            message            = Message("messages__whyNoUTR", trusteeName),
            url                = Some(routes.TrusteeNoUTRReasonController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__whyNoUTR__cya__visuallyHidden".withArgs(trusteeName))
          )
      }
    ).flatten
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}
