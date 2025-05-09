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

package helpers.cya.establishers.company

import controllers.establishers.company.director.address.{routes => addressRoutes}
import controllers.establishers.company.director.contact.{routes => contactRoutes}
import controllers.establishers.company.director.details.{routes => detailsRoutes}
import controllers.establishers.company.director.routes
import helpers.cya.CYAHelper
import helpers.cya.CYAHelper.getName
import identifiers.establishers.company.director._
import identifiers.establishers.company.director.address.{AddressId, AddressYearsId, PreviousAddressId}
import identifiers.establishers.company.director.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.company.director.details._
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.{Enumerable, UserAnswers}

class EstablisherCompanyDirectorDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  //scalastyle:off method.length
  def detailsRows(
                   establisherIndex: Index, directorIndex: Index
                 )(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[SummaryListRow] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val directorName: String =
      getName(DirectorNameId(establisherIndex,directorIndex))

    val rowsWithoutDynamicIndices = Seq(
      Some(answerOrAddRow(
        id                 = DirectorNameId(establisherIndex, directorIndex),
        message            = Messages("messages__director__name"),
        url                = Some(routes.DirectorNameController.onPageLoad(establisherIndex,directorIndex, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__director__name__cya__visuallyHidden", directorName))),
        answerTransform    = answerPersonNameTransform
      )),

      Some(answerOrAddRow(
        id                 = DirectorDOBId(establisherIndex, directorIndex),
        message            = Messages("messages__dob__h1", directorName),
        url                = Some(detailsRoutes.DirectorDOBController.onPageLoad(establisherIndex,directorIndex,CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__dob__cya__visuallyHidden", directorName))),
        answerTransform = answerDateTransform
      )),
      Some(answerOrAddRow(
        id                 = DirectorHasNINOId(establisherIndex, directorIndex),
        message            = Messages("messages__hasNINO", directorName),
        url                = Some(detailsRoutes.DirectorHasNINOController.onPageLoad(establisherIndex, directorIndex, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__hasNINO__cya__visuallyHidden", directorName))),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(DirectorHasNINOId(establisherIndex, directorIndex)) map {
        case true =>
          answerOrAddRow(
            id                 = DirectorNINOId(establisherIndex, directorIndex),
            message            = Messages("messages__enterNINO__cya", directorName),
            url                = Some(detailsRoutes.DirectorEnterNINOController.onPageLoad(establisherIndex, directorIndex, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__hasNINO__cya__visuallyHidden", directorName))),
            answerTransform    = referenceValueTransform
          )
        case false =>
          answerOrAddRow(
            id                 = DirectorNoNINOReasonId(establisherIndex, directorIndex),
            message            = Messages("messages__whyNoNINO", directorName),
            url                = Some(detailsRoutes.DirectorNoNINOReasonController.onPageLoad(establisherIndex, directorIndex, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__whyNoNINO__cya__visuallyHidden", directorName)))
          )
      },
      Some(answerOrAddRow(
        id                 = DirectorHasUTRId(establisherIndex, directorIndex),
        message            = Messages("messages__hasUTR", directorName),
        url                = Some(detailsRoutes.DirectorHasUTRController.onPageLoad(establisherIndex, directorIndex,CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__hasUTR__cya__visuallyHidden", directorName))),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(DirectorHasUTRId(establisherIndex, directorIndex)) map {
        case true =>
          answerOrAddRow(
            id                 = DirectorEnterUTRId(establisherIndex, directorIndex),
            message            = Messages("messages__enterUTR__cya_label", directorName),
            url                = Some(detailsRoutes.DirectorEnterUTRController.onPageLoad(establisherIndex, directorIndex,CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__hasUTR__cya__visuallyHidden", directorName))),
            answerTransform    = referenceValueTransform
          )
        case false =>
          answerOrAddRow(
            id                 = DirectorNoUTRReasonId(establisherIndex, directorIndex),
            message            = Messages("messages__whyNoUTR", directorName),
            url                = Some(detailsRoutes.DirectorNoUTRReasonController.onPageLoad(establisherIndex, directorIndex,CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__whyNoUTR__cya__visuallyHidden", directorName)))
          )
      },
      Some( answerOrAddRow(
            id                  = AddressId(establisherIndex, directorIndex),
            message             = Messages("addressList_cya_label", directorName),
            url                 = Some(addressRoutes.EnterPostcodeController.onPageLoad(establisherIndex, directorIndex,  CheckMode).url),
            visuallyHiddenText  = Some(Text(Messages("messages__visuallyHidden__address", directorName))), answerAddressTransform
          ))
     ,
      Some(
        answerOrAddRow(
            id                  = AddressYearsId(establisherIndex, directorIndex),
            message             = Messages("addressYears.title", directorName),
            url                 = Some(addressRoutes.AddressYearsController.onPageLoad(establisherIndex, directorIndex, CheckMode).url),
            visuallyHiddenText  = Some(Text(Messages("messages__visuallyhidden__addressYears", directorName))), answerBooleanTransform
          ))
      ,
      if (ua.get(AddressYearsId(establisherIndex, directorIndex)).contains(true)) {
        None
      }else{
        Some( answerOrAddRow(
          id = PreviousAddressId(establisherIndex, directorIndex),
          message = Messages("previousAddressList_cya_label", directorName),
          url = Some(addressRoutes.EnterPreviousPostcodeController.onPageLoad(establisherIndex, directorIndex, CheckMode).url),
          visuallyHiddenText = Some(Text(Messages("messages__visuallyHidden__previousAddress", directorName))), answerAddressTransform
        ))
      },
      Some(answerOrAddRow(
        id                  = EnterEmailId(establisherIndex, directorIndex),
        message             = Messages("messages__enterEmail_cya_label", directorName),
        url                 = Some(contactRoutes.EnterEmailController.onPageLoad(establisherIndex, directorIndex,CheckMode).url),
        visuallyHiddenText  = Some(Text(Messages("messages__enterEmail__cya__visuallyHidden", directorName)))
      )),
      Some(answerOrAddRow(
        id                  = EnterPhoneId(establisherIndex, directorIndex),
        message             = Messages("messages__enterPhone_cya_label", directorName),
        url                 = Some(contactRoutes.EnterPhoneNumberController.onPageLoad(establisherIndex, directorIndex, CheckMode).url),
        visuallyHiddenText  = Some(Text(Messages("messages__enterPhone__cya__visuallyHidden", directorName)))
      ))
    ).flatten
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}
