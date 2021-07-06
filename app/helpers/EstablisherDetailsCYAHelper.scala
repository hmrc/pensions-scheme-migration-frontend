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

package helpers

import controllers.establishers.individual.details.routes
import helpers.CYAHelper.getName
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.details._
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.MessageInterpolators
import uk.gov.hmrc.viewmodels.SummaryList.Row
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class EstablisherDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  def detailsRows(
                   index: Index
                 )(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[Row] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val establisherName: String =
      getName(EstablisherNameId(index))

    Seq(
      Some(answerOrAddRow(
        id                 = EstablisherDOBId(index),
        message            = Message("messages__dob__title", establisherName).resolve,
        url                = Some(routes.EstablisherDOBController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__dob__cya__visuallyHidden".withArgs(establisherName)),
        answerTransform = answerDateTransform
      )),
      Some(answerOrAddRow(
        id                 = EstablisherHasNINOId(index),
        message            = Message("messages__hasNINO", establisherName).resolve,
        url                = Some(routes.EstablisherHasNINOController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(establisherName)),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(EstablisherNINOId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = EstablisherNINOId(index),
            message            = Message("messages__hasNINO__cya", establisherName),
            url                = Some(routes.EstablisherEnterNINOController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__hasNINO__cya__visuallyHidden".withArgs(establisherName)),
            answerTransform    = referenceValueTransform
          )
      },
      ua.get(EstablisherNoNINOReasonId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = EstablisherNoNINOReasonId(index),
            message            = Message("messages__whyNoNINO", establisherName),
            url                = Some(routes.EstablisherNoNINOReasonController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__whyNoNINO__cya__visuallyHidden".withArgs(establisherName))
          )
      },
      Some(answerOrAddRow(
        id                 = EstablisherHasUTRId(index),
        message            = Message("messages__hasUTR", establisherName).resolve,
        url                = Some(routes.EstablisherHasUTRController.onPageLoad(index, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(establisherName)),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(EstablisherUTRId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = EstablisherUTRId(index),
            message            = Message("messages__hasUTR__cya", establisherName),
            url                = Some(routes.EstablisherEnterUTRController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(establisherName)),
            answerTransform    = referenceValueTransform
          )
      },
      ua.get(EstablisherNoUTRReasonId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = EstablisherNoUTRReasonId(index),
            message            = Message("messages__whyNoUTR", establisherName),
            url                = Some(routes.EstablisherNoUTRReasonController.onPageLoad(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__whyNoUTR__cya__visuallyHidden".withArgs(establisherName))
          )
      }
    ).flatten
  }
}
