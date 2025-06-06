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

package helpers.cya.establishers.individual

import helpers.cya.CYAHelper
import helpers.cya.CYAHelper.getName
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.details._
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.{Enumerable, UserAnswers}

class EstablisherDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {
  //scalastyle:off method.length
  def detailsRows(
                   index: Index
                 )(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[SummaryListRow] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val establisherName: String =
      getName(EstablisherNameId(index))

    val rowsWithoutDynamicIndices = Seq(
      Some(answerOrAddRow(
        id                 = EstablisherDOBId(index),
        message            = Messages("messages__dob__h1", establisherName),
        url                = Some(controllers.establishers.individual.details.routes.EstablisherDOBController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__dob__cya__visuallyHidden", establisherName))),
        answerTransform = answerDateTransform
      )),
      Some(answerOrAddRow(
        id                 = EstablisherHasNINOId(index),
        message            = Messages("messages__hasNINO", establisherName),
        url                = Some(controllers.establishers.individual.details.routes.EstablisherHasNINOController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__hasNINO__cya__visuallyHidden", establisherName))),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(EstablisherHasNINOId(index)) map {
        case true =>
          answerOrAddRow(
            id                 = EstablisherNINOId(index),
            message            = Messages("messages__enterNINO__cya", establisherName),
            url                = Some(controllers.establishers.individual.details.routes.EstablisherEnterNINOController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__enterNINO__cya_visuallyHidden", establisherName))),
            answerTransform    = referenceValueTransform
          )
      case false =>
          answerOrAddRow(
            id                 = EstablisherNoNINOReasonId(index),
            message            = Messages("messages__whyNoNINO", establisherName),
            url                = Some(controllers.establishers.individual.details.routes.EstablisherNoNINOReasonController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__whyNoNINO__cya__visuallyHidden", establisherName)))
          )
      },
      Some(answerOrAddRow(
        id                 = EstablisherHasUTRId(index),
        message            = Messages("messages__hasUTR", establisherName),
        url                = Some(controllers.establishers.individual.details.routes.EstablisherHasUTRController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__hasUTR__cya__visuallyHidden", establisherName))),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(EstablisherHasUTRId(index)) map {
        case true =>
          answerOrAddRow(
            id                 = EstablisherUTRId(index),
            message            = Messages("messages__enterUTR__cya_label", establisherName),
            url                = Some(controllers.establishers.individual.details.routes.EstablisherEnterUTRController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__enterUTR__cya_visuallyHidden", establisherName))),
            answerTransform    = referenceValueTransform
          )
        case false =>
          answerOrAddRow(
            id                 = EstablisherNoUTRReasonId(index),
            message            = Messages("messages__whyNoUTR", establisherName),
            url                = Some(controllers.establishers.individual.details.routes.EstablisherNoUTRReasonController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__whyNoUTR__cya__visuallyHidden", establisherName)))
          )
      }
    ).flatten
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}
