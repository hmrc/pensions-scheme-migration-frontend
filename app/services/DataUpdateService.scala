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

package services

import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.details._
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.details._
import identifiers.trustees.{TrusteeKindId, TrusteesId}
import models.prefill.IndividualDetails
import models.trustees.TrusteeKind
import models.{Index, PersonName, ReferenceValue}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.{Enumerable, UserAnswers}

import java.time.LocalDate
import javax.inject.Inject

class DataUpdateService @Inject()() extends Enumerable.Implicits {

  def allIndividualTrustees(implicit ua: UserAnswers): Seq[IndividualDetails] = {
    ua.data.validate[Seq[Option[IndividualDetails]]](readsTrustees) match {
      case JsSuccess(trustees, _) =>
        trustees.flatten
      case JsError(_) =>
        Nil
    }
  }

  private def readsTrustees(implicit ua: UserAnswers): Reads[Seq[Option[IndividualDetails]]] = new Reads[Seq[Option[IndividualDetails]]] {
    private def readsIndividualTrustee(index: Int)(implicit ua: UserAnswers): Reads[Option[IndividualDetails]] = (
      (JsPath \ TrusteeNameId.toString).read[PersonName] and
        (JsPath \ TrusteeDOBId.toString).readNullable[LocalDate] and
        (JsPath \ TrusteeNINOId.toString).readNullable[ReferenceValue]
      ) ((trusteeName, dob, ninoReferenceVale) =>
      Some(IndividualDetails(
        trusteeName.firstName, trusteeName.lastName, trusteeName.isDeleted, ninoReferenceVale.map(_.value), dob, index, ua.isTrusteeIndividualComplete(index)))
    )

    override def reads(json: JsValue): JsResult[Seq[Option[IndividualDetails]]] = {
      ua.data \ TrusteesId.toString match {
        case JsDefined(JsArray(trustees)) =>
          val jsResults: IndexedSeq[JsResult[Option[IndividualDetails]]] = trustees.zipWithIndex.map { case (jsValue, index) =>
            val trusteeKind = (jsValue \ TrusteeKindId.toString).validate[String].asOpt
            val readsForTrusteeKind = trusteeKind match {
              case Some(TrusteeKind.Individual.toString) => readsIndividualTrustee(index)
              case _ => Reads.pure[Option[IndividualDetails]](None)
            }
            readsForTrusteeKind.reads(jsValue)
          }
          asJsResultSeq(jsResults)
        case _ => JsSuccess(Nil)
      }
    }
  }


  def findMatchingTrustee(establisherIndex: Index, directorIndex: Index)(implicit ua: UserAnswers): IndividualDetails = {
    val filteredTrusteesSeq = allIndividualTrustees.filter(indv => !indv.isDeleted)
    val director = getDirectorDetails(establisherIndex, directorIndex)
    filteredTrusteesSeq.filter { trustee =>
      trustee.nino.map(ninoVal => director.nino.contains(ninoVal))
        .getOrElse {(trustee.dob, director.dob) match {
            case (Some(trusteeDob), Some(dirDob)) => dirDob.isEqual(trusteeDob) && trustee.fullName == director.fullName
            case _ => false
          }
        }
    }
    filteredTrusteesSeq(0)
  }


  private def getDirectorDetails(estIndex: Int, directorIndex: Int)(implicit ua: UserAnswers): IndividualDetails = {
    val directorName = ua.get(DirectorNameId(estIndex, directorIndex)).get
    val directorNino = ua.get(DirectorNINOId(estIndex, directorIndex)).map(_.value)
    val directorDob = ua.get(DirectorDOBId(estIndex, directorIndex))
    IndividualDetails(
      directorName.firstName, directorName.lastName, directorName.isDeleted, directorNino, directorDob, directorIndex,
      ua.isDirectorComplete(estIndex, directorIndex), Some(estIndex))
  }

  private def asJsResultSeq[A](jsResults: Seq[JsResult[A]]): JsResult[Seq[A]] = {
    JsSuccess(jsResults.collect {
      case JsSuccess(i, _) => i
    })
  }
}
