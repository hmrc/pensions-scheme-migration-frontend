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

import controllers.establishers.company.director.routes
import helpers.CYAHelper.getName
import identifiers.establishers.company.director._
import identifiers.establishers.company.director.address.{AddressId, AddressYearsId, PreviousAddressId}
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.MessageInterpolators
import uk.gov.hmrc.viewmodels.SummaryList.Row
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class EstablisherCompanyDirectorDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  def detailsRows(
                   establisherIndex: Index, directorIndex: Index
                 )(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[Row] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val directorName: String =
      getName(DirectorNameId(establisherIndex,directorIndex))

    Seq(
      Some(answerOrAddRow(
        id                 = DirectorNameId(establisherIndex, directorIndex),
        message            = Message("messages__director__name").resolve,
        url                = Some(routes.DirectorNameController.onPageLoad(establisherIndex,directorIndex).url),
        visuallyHiddenText = Some(msg"messages__director__name__cya__visuallyHidden".withArgs(directorName)),
        answerTransform    =answerPersonNameTransform
      )),

      Some(answerOrAddRow(
        id                 =DirectorDOBId(establisherIndex, directorIndex),
        message            = Message("messages__dob__h1", directorName).resolve,
        url                = Some(controllers.establishers.company.director.details.routes.DirectorDOBController.onPageLoad(establisherIndex,directorIndex,CheckMode).url),
        visuallyHiddenText = Some(msg"messages__dob__cya__visuallyHidden".withArgs(directorName)),
        answerTransform = answerDateTransform
      )),
      Some(answerOrAddRow(
        id                 = DirectorHasNINOId(establisherIndex, directorIndex),
        message            = Message("messages__hasNINO", directorName).resolve,
        url                = Some(controllers.establishers.company.director.details.routes.DirectorHasNINOController.onPageLoad(establisherIndex, directorIndex, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(directorName)),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(DirectorNINOId(establisherIndex, directorIndex)) map {
        _ =>
          answerOrAddRow(
            id                 = DirectorNINOId(establisherIndex, directorIndex),
            message            = Message("messages__hasNINO__cya", directorName),
            url                = Some(controllers.establishers.company.director.details.routes.DirectorEnterNINOController.onPageLoad(establisherIndex, directorIndex, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__hasNINO__cya__visuallyHidden".withArgs(directorName)),
            answerTransform    = referenceValueTransform
          )
      },
      ua.get(DirectorNoNINOReasonId(establisherIndex, directorIndex)) map {
        _ =>
          answerOrAddRow(
            id                 = DirectorNoNINOReasonId(establisherIndex, directorIndex),
            message            = Message("messages__whyNoNINO", directorName),
            url                = Some(controllers.establishers.company.director.details.routes.DirectorNoNINOReasonController.onPageLoad(establisherIndex, directorIndex, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__whyNoNINO__cya__visuallyHidden".withArgs(directorName))
          )
      },
      Some(answerOrAddRow(
        id                 = DirectorHasUTRId(establisherIndex, directorIndex),
        message            = Message("messages__hasUTR", directorName).resolve,
        url                = Some(controllers.establishers.company.director.details.routes.DirectorHasUTRController.onPageLoad(establisherIndex, directorIndex,CheckMode).url),
        visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(directorName)),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(DirectorEnterUTRId(establisherIndex, directorIndex)) map {
        _ =>
          answerOrAddRow(
            id                 = DirectorEnterUTRId(establisherIndex, directorIndex),
            message            = Message("messages__hasUTR__cya", directorName),
            url                = Some(controllers.establishers.company.director.details.routes.DirectorEnterUTRController.onPageLoad(establisherIndex, directorIndex,CheckMode).url),
            visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(directorName)),
            answerTransform    = referenceValueTransform
          )
      },
      ua.get(DirectorNoUTRReasonId(establisherIndex, directorIndex)) map {
        _ =>
          answerOrAddRow(
            id                 = DirectorNoUTRReasonId(establisherIndex, directorIndex),
            message            = Message("messages__whyNoUTR", directorName),
            url                = Some(controllers.establishers.company.director.details.routes.DirectorNoUTRReasonController.onPageLoad(establisherIndex, directorIndex,CheckMode).url),
            visuallyHiddenText = Some(msg"messages__whyNoUTR__cya__visuallyHidden".withArgs(directorName))
          )
      },
      ua.get(AddressId(establisherIndex, directorIndex)) map {
        _ =>
          answerOrAddRow(
            id                  = AddressId(establisherIndex, directorIndex),
            message             = Message("messages__establisherAddress__whatYouWillNeed_h1", directorName).resolve,
            url                 = Some(controllers.establishers.company.director.address.routes.EnterPostcodeController.onPageLoad(establisherIndex, directorIndex).url),
            visuallyHiddenText  = Some(msg"messages__visuallyHidden__address".withArgs(directorName)), answerAddressTransform
          )
      },
      ua.get(AddressYearsId(establisherIndex, directorIndex)) map {
        _ =>
          answerOrAddRow(
            id                  = AddressYearsId(establisherIndex, directorIndex),
            message             = Message("addressYears.title", directorName).resolve,
            url                 = Some(controllers.establishers.company.director.address.routes.AddressYearsController.onPageLoad(establisherIndex, directorIndex).url),
            visuallyHiddenText  = Some(msg"messages__visuallyhidden__addressYears".withArgs(directorName)), answerBooleanTransform
          )
      },
      ua.get(PreviousAddressId(establisherIndex, directorIndex)) map {
        _ =>
          answerOrAddRow(
            id                  = PreviousAddressId(establisherIndex, directorIndex),
            message             = Message("messages__establisherPreviousAddress").resolve,
            url                 = Some(controllers.establishers.company.director.address.routes.EnterPreviousPostcodeController.onPageLoad(establisherIndex, directorIndex).url),
            visuallyHiddenText  = Some(msg"messages__visuallyHidden__previousAddress".withArgs(directorName))
          )
      },
      Some(answerOrAddRow(
        id                  = DirectorEmailId(establisherIndex, directorIndex),
        message             = Message("messages__enterEmail", directorName).resolve,
        url                 = Some(controllers.establishers.company.director.contact.routes.EnterEmailController.onPageLoad(establisherIndex, directorIndex,CheckMode).url),
        visuallyHiddenText  = Some(msg"messages__enterEmail__cya__visuallyHidden".withArgs(directorName))
      )),
      Some(answerOrAddRow(
        id                  = DirectorPhoneNumberId(establisherIndex, directorIndex),
        message             = Message("messages__enterPhone", directorName).resolve,
        url                 = Some(controllers.establishers.company.director.contact.routes.EnterPhoneNumberController.onPageLoad(establisherIndex, directorIndex, CheckMode).url),
        visuallyHiddenText  = Some(msg"messages__enterPhone__cya__visuallyHidden".withArgs(directorName))
      ))
    ).flatten
  }
}
