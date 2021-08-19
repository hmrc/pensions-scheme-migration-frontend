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

package utils.datacompletion

import identifiers._
import identifiers.aboutMembership.{FutureMembersId, CurrentMembersId}
import identifiers.beforeYouStart._
import identifiers.benefitsAndInsurance._
import models.benefitsAndInsurance.BenefitsProvisionType.DefinedBenefitsOnly
import play.api.libs.json.Reads
import utils.UserAnswers

trait DataCompletion {

  self: UserAnswers =>

  def isListComplete(list: Seq[Boolean]): Boolean =
    list.nonEmpty & list.foldLeft(true)({
      case (acc, true) => acc
      case (_, false) => false
    })

  def isBeforeYouStartCompleted: Boolean = {
      Seq(
        get(SchemeNameId).isDefined,
        get(SchemeTypeId).isDefined,
        get(EstablishedCountryId).isDefined,
        get(WorkingKnowledgeId).isDefined
      ).forall(identity)
  }

  def isMembersCompleted: Option[Boolean] = isComplete(Seq(
    isAnswerComplete(CurrentMembersId),
    isAnswerComplete(FutureMembersId)))

  def isBenefitsAndInsuranceCompleted: Option[Boolean] = {
    val benefitsTypeCompletion =
      if (get(HowProvideBenefitsId).contains(DefinedBenefitsOnly)) {
        Nil
      } else {
        Seq(get(BenefitsTypeId).map(_=>true))
      }
    val policyDetailsCompletion =
      if (get(AreBenefitsSecuredId).contains(true)) {
        Seq(
            get(BenefitsInsuranceNameId).map(_=>true),
            get(BenefitsInsurancePolicyId).map(_=>true),
            get(InsurerAddressId).map(_=>true)
        )
      } else {
        Nil
      }

    isComplete(
        Seq(
            get(HowProvideBenefitsId).map(_=>true),
            isAnswerComplete(AreBenefitsSecuredId)
        ) ++ benefitsTypeCompletion ++ policyDetailsCompletion
      )
    }

  //GENERIC METHODS
  def isComplete(list: Seq[Option[Boolean]]): Option[Boolean] =
    if (list.flatten.isEmpty) None
    else
      Some(list.foldLeft(true)({
        case (acc, Some(true)) => acc
        case (_, Some(false)) => false
        case (_, None) => false
      }))

  def isAnswerComplete[A](id: TypedIdentifier[A])(implicit rds: Reads[A]): Option[Boolean] =
    get(id) match {
      case None => None
      case Some(_) => Some(true)
    }

  def isAnswerComplete[A](yesNoQuestionId: TypedIdentifier[Boolean],
                          yesValueId: TypedIdentifier[A],
                          noReasonIdOpt: Option[TypedIdentifier[String]])(implicit reads: Reads[A]): Option[Boolean] =
    (get(yesNoQuestionId), get(yesValueId), noReasonIdOpt) match {
      case (None, None, _) => None
      case (_, Some(_), _) => Some(true)
      case (_, _, Some(noReasonId)) if get(noReasonId).isDefined => Some(true)
      case (Some(false), _, None) => Some(true)
      case _ => Some(false)
    }

  def isContactDetailsComplete(emailId: TypedIdentifier[String],
                               phoneId: TypedIdentifier[String]): Option[Boolean] =
    (get(emailId), get(phoneId)) match {
      case (Some(_), Some(_)) => Some(true)
      case (None, None) => None
      case _ => Some(false)
    }
}
