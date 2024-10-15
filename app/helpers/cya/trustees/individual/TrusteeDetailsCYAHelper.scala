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

import controllers.trustees.individual.details.routes
import helpers.cya.CYAHelper
import helpers.cya.CYAHelper.getName
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.details._
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.{Enumerable, UserAnswers}

class TrusteeDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  def detailsRows(index: Index)
                 (implicit request: DataRequest[AnyContent],
                   messages: Messages): Seq[SummaryListRow] = {
    implicit val ua: UserAnswers = request.userAnswers
    val trusteeName: String = getName(TrusteeNameId(index))

    val rowsWithoutDynamicIndices = Seq(
      Some(answerOrAddRow(
        id                 = TrusteeDOBId(index),
        message            = Messages("messages__dob__h1", trusteeName),
        url                = Some(routes.TrusteeDOBController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__dob__cya__visuallyHidden", trusteeName))),
        answerTransform = answerDateTransform
      )),
      Some(answerOrAddRow(
        id                 = TrusteeHasNINOId(index),
        message            = Messages("messages__hasNINO", trusteeName),
        url                = Some(routes.TrusteeHasNINOController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__hasUTR__cya__visuallyHidden", trusteeName))),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(TrusteeHasNINOId(index)) map {
        case true =>
          answerOrAddRow(
            id                 = TrusteeNINOId(index),
            message            = Messages("messages__enterNINO__cya", trusteeName),
            url                = Some(routes.TrusteeEnterNINOController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__hasNINO__cya__visuallyHidden", trusteeName))),
            answerTransform    = referenceValueTransform
          )
        case false =>
          answerOrAddRow(
            id                 = TrusteeNoNINOReasonId(index),
            message            = Messages("messages__whyNoNINO", trusteeName),
            url                = Some(routes.TrusteeNoNINOReasonController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__whyNoNINO__cya__visuallyHidden", trusteeName)))
          )
      },
      Some(answerOrAddRow(
        id                 = TrusteeHasUTRId(index),
        message            = Messages("messages__hasUTR", trusteeName),
        url                = Some(routes.TrusteeHasUTRController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__hasUTR__cya__visuallyHidden", trusteeName))),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(TrusteeHasUTRId(index)) map {
        case true =>
          answerOrAddRow(
            id                 = TrusteeUTRId(index),
            message            = Messages("messages__enterUTR__cya_label", trusteeName),
            url                = Some(routes.TrusteeEnterUTRController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__hasUTR__cya__visuallyHidden", trusteeName))),
            answerTransform    = referenceValueTransform
          )
        case false =>
          answerOrAddRow(
            id                 = TrusteeNoUTRReasonId(index),
            message            = Messages("messages__whyNoUTR", trusteeName),
            url                = Some(routes.TrusteeNoUTRReasonController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__whyNoUTR__cya__visuallyHidden", trusteeName)))
          )
      }
    ).flatten
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}
