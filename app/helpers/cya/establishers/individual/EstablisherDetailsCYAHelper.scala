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
import identifiers.establishers.individual.details._
import models.requests.DataRequest
import models.{Index, CheckMode}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.MessageInterpolators
import uk.gov.hmrc.viewmodels.SummaryList.Row
import utils.{UserAnswers, Enumerable}
import viewmodels.Message
import EstablishersIndividualRoutes._

class EstablisherDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {
  //scalastyle:off method.length
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

    val rowsWithoutDynamicIndices = Seq(
      Some(answerOrAddRow(
        id                 = EstablisherDOBId(index),
        message            = Message("messages__dob__h1", establisherName).resolve,
        url                = Some(dateOfBirthRoute(index, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__dob__cya__visuallyHidden".withArgs(establisherName)),
        answerTransform = answerDateTransform
      )),
      Some(answerOrAddRow(
        id                 = EstablisherHasNINOId(index),
        message            = Message("messages__hasNINO", establisherName).resolve,
        url                = Some(haveNationalInsuranceNumberRoute(index, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__hasNINO__cya__visuallyHidden".withArgs(establisherName)),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(EstablisherNINOId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = EstablisherNINOId(index),
            message            = Message("messages__enterNINO__cya", establisherName),
            url                = Some(enterUniqueTaxpayerReferenceRoute(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__enterNINO__cya_visuallyHidden".withArgs(establisherName)),
            answerTransform    = referenceValueTransform
          )
      },
      ua.get(EstablisherNoNINOReasonId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = EstablisherNoNINOReasonId(index),
            message            = Message("messages__whyNoNINO", establisherName),
            url                = Some(reasonForNoNationalInsuranceNumberRoute(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__whyNoNINO__cya__visuallyHidden".withArgs(establisherName))
          )
      },
      Some(answerOrAddRow(
        id                 = EstablisherHasUTRId(index),
        message            = Message("messages__hasUTR", establisherName).resolve,
        url                = Some(haveUniqueTaxpayerReferenceRoute(index, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(establisherName)),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(EstablisherUTRId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = EstablisherUTRId(index),
            message            = Message("messages__enterUTR__cya_label", establisherName),
            url                = Some(enterUniqueTaxpayerReferenceRoute(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__enterUTR__cya_visuallyHidden".withArgs(establisherName)),
            answerTransform    = referenceValueTransform
          )
      },
      ua.get(EstablisherNoUTRReasonId(index)) map {
        _ =>
          answerOrAddRow(
            id                 = EstablisherNoUTRReasonId(index),
            message            = Message("messages__whyNoUTR", establisherName),
            url                = Some(reasonForNoUniqueTaxpayerReferenceRoute(index, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__whyNoUTR__cya__visuallyHidden".withArgs(establisherName))
          )
      }
    ).flatten
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}
