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

package services

import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.details.*
import identifiers.establishers.{EstablisherKindId, EstablishersId}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.details.*
import identifiers.trustees.{IsTrusteeNewId, TrusteeKindId, TrusteesId}
import models.establishers.EstablisherKind
import models.prefill.IndividualDetails
import models.trustees.TrusteeKind
import models.{PersonName, ReferenceValue}
import play.api.Logging
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import play.api.libs.json.Reads.JsObjectReducer
import utils.{DataCleanUp, Enumerable, UserAnswers}

import java.time.LocalDate
import javax.inject.Inject
import scala.collection.Set
import scala.language.postfixOps

class DataPrefillService @Inject() extends Enumerable.Implicits with Logging {

  def copyAllDirectorsToTrustees(ua: UserAnswers, seqIndexes: Seq[Int], establisherIndex: Int): UserAnswers = {
    val seqDirectors: Seq[JsObject] =
      (ua.data \ EstablishersId.toString \ establisherIndex \ "director").validate[JsArray].asOpt match {
        case Some(arr) =>
          seqIndexes.map { index =>
            arr.value(index).transform(copyDirectorToTrustee) match {
              case JsSuccess(value, _) =>
                value
              case JsError(errors) =>
                throw Exception(
                  "copyAllDirectorsToTrustees copyDirectorToTrustee failed:" +
                    s"\npath(s) from JSON: ${errors.map(_._1.path.mkString(", "))}" +
                    s"\nerror messages from JSON: ${errors.flatMap(_._2.map(_.messages.head))}"
                )
            }
          }
        case _ =>
          Nil
      }

    val trusteeTransformer: Reads[JsObject] =
      (__ \ TrusteesId.toString).json.update(__.read[JsArray].map {
        case existingTrustees@JsArray(_) =>
          JsArray(filterTrusteeIndividuals(existingTrustees) ++ seqDirectors)
      }
    )

    transformUa(ua, trusteeTransformer, "trusteeTransformer")
  }

  def copyAllTrusteesToDirectors(ua: UserAnswers, seqIndexes: Seq[Int], establisherIndex: Int): UserAnswers = {
    val completeNotDeletedTrusteeIndexes: Seq[Int] =
      allIndividualTrustees(ua).zipWithIndex.flatMap { case (trustee, index) =>
        if (!trustee.isDeleted && trustee.isComplete) Some(index) else None
      }
    
    val seqTrustees: Seq[JsObject] =
      (ua.data \ TrusteesId.toString).validate[JsArray].asOpt match {
        case Some(arr) =>

          val trusteeIndividualArray: collection.IndexedSeq[JsValue] =
            filterTrusteeIndividuals(arr)
            
          val completeNotDeletedTrustees: Seq[JsValue] =
            completeNotDeletedTrusteeIndexes.map(trusteeIndividualArray(_))
            
          seqIndexes.map { index =>
            completeNotDeletedTrustees(index).transform(copyTrusteeToDirector) match {
              case JsSuccess(value, _) =>
                value
              case JsError(errors) =>
                throw Exception(
                  "copyAllTrusteesToDirectors copyTrusteeToDirector failed:" +
                    s"\npath(s) from JSON: ${errors.map(_._1.path.mkString(", "))}" +
                    s"\nerror messages from JSON: ${errors.flatMap(_._2.map(_.messages.head))}"
                )
            }
          }
        case _ =>
          Nil
      }
      
    val seqDirectors: collection.Seq[JsValue] =
      (ua.data \ EstablishersId.toString \ establisherIndex \ "director").validate[JsArray].asOpt match {
        case Some(arr) => arr.value
        case _ => Nil
      }

    val establisherTransformer: Reads[JsObject] =
      (__ \ EstablishersId.toString).json.update(__.read[JsArray].map { arr =>
        JsArray(
          arr.value.updated(
            establisherIndex,
            arr(establisherIndex).as[JsObject] ++ Json.obj("director" -> (seqDirectors ++ seqTrustees))
          )
        )
      })

    transformUa(ua, establisherTransformer, "establisherTransformer")
  }

  private def transformUa(ua: UserAnswers, transformer: Reads[JsObject], transformerName: String): UserAnswers =
    ua.data.transform(transformer) match {
      case JsSuccess(value, _) =>
        UserAnswers(value)
      case JsError(errors) =>
        logger.error(
          s"transformUa $transformerName failed:" +
            s"\npath(s) from JSON: ${errors.map(_._1.path.mkString(", "))}" +
            s"\nerror messages from JSON: ${errors.flatMap(_._2.map(_.messages.head))}"
        )
        ua
    }

  private def copyDirectorToTrustee: Reads[JsObject] = {
    (__ \ "trusteeDetails" \ "firstName").json.copyFrom((__ \ "directorDetails" \ "firstName").json.pick) and
      (__ \ "trusteeDetails" \ "lastName").json.copyFrom((__ \ "directorDetails" \ "lastName").json.pick) and
      (__ \ "trusteeKind").json.put(JsString("individual")) and
      (__ \ "phone").json.copyFrom((__ \ "directorContactDetails" \ "phoneNumber").json.pick) and
      (__ \ "email").json.copyFrom((__ \ "directorContactDetails" \ "emailAddress").json.pick) and
      commonReads reduce
  }

  private def copyTrusteeToDirector: Reads[JsObject] = {
    (__ \ "directorDetails" \ "firstName").json.copyFrom((__ \ "trusteeDetails" \ "firstName").json.pick) and
      (__ \ "directorDetails" \ "lastName").json.copyFrom((__ \ "trusteeDetails" \ "lastName").json.pick) and
      (__ \ "directorContactDetails" \ "phoneNumber").json.copyFrom((__ \ "phone").json.pick) and
      (__ \ "directorContactDetails" \ "emailAddress").json.copyFrom((__ \ "email").json.pick) and
      commonReads reduce
  }

  private def commonReads: Reads[JsObject] = {
    (__ \ "dateOfBirth").json.copyFrom((__ \ "dateOfBirth").json.pick) and
      (__ \ "address").json.copyFrom((__ \ "address").json.pick) and
      (__ \ "addressYears").json.copyFrom((__ \ "addressYears").json.pick) and
      ((__ \ "previousAddress").json.copyFrom((__ \ "previousAddress").json.pick) orElse __.json.put(Json.obj())) and
      (__ \ "hasUtr").read[Boolean].flatMap {
        case true =>
          (__ \ "hasUtr").json.copyFrom((__ \ "hasUtr").json.pick) and
            (__ \ "utr").json.copyFrom((__ \ "utr").json.pick) reduce // Remove [JsObject]
        case false =>
          (__ \ "hasUtr").json.copyFrom((__ \ "hasUtr").json.pick) and
            (__ \ "noUtrReason").json.copyFrom((__ \ "noUtrReason").json.pick) reduce // Remove [JsObject]
      } and
      (__ \ "hasNino").read[Boolean].flatMap {
        case true =>
          (__ \ "hasNino").json.copyFrom((__ \ "hasNino").json.pick) and
            (__ \ "nino").json.copyFrom((__ \ "nino").json.pick) reduce // Remove [JsObject]
        case false =>
          (__ \ "hasNino").json.copyFrom((__ \ "hasNino").json.pick) and
            (__ \ "noNinoReason").json.copyFrom((__ \ "noNinoReason").json.pick) reduce // Remove [JsObject]
      } reduce // Remove [JsObject]
  }
  
  def getListOfDirectorsToBeCopied(implicit ua: UserAnswers): Seq[IndividualDetails] = {
    val filteredDirectorsSeq: Seq[IndividualDetails] =
      allDirectors.filter(dir => !dir.isDeleted && dir.isComplete)
    
    filteredDirectorsSeq.filterNot { director =>
      val allNonDeletedTrustees = allIndividualTrustees.filter(!_.isDeleted)

      director
        .nino
        .map(ninoVal => allNonDeletedTrustees.exists(_.nino.contains(ninoVal)))
        .getOrElse(
          allNonDeletedTrustees.exists { trustee =>
            (trustee.dob, director.dob) match {
              case (Some(trusteeDob), Some(dirDob)) =>
                trusteeDob.isEqual(dirDob) && trustee.fullName == director.fullName
              case _ =>
                false
            }
          }
        )
    }
  }

  def getListOfTrusteesToBeCopied(establisherIndex: Int)(implicit ua: UserAnswers): Seq[IndividualDetails] = {
    val completeNotDeletedTrustees: Seq[IndividualDetails] =
      allIndividualTrustees.filter(trustee => !trustee.isDeleted && trustee.isComplete)

    val completeNotDeletedDirectors: collection.Seq[IndividualDetails] =
      directors(establisherIndex).filter(director => !director.isDeleted && director.isComplete)

    completeNotDeletedTrustees.filterNot { trustee =>
      trustee
        .nino
        .map(ninoVal => completeNotDeletedDirectors.exists(_.nino.contains(ninoVal)))
        .getOrElse(
          completeNotDeletedDirectors.exists { director =>
            (trustee.dob, director.dob) match {
              case (Some(trusteeDob), Some(dirDob)) =>
                dirDob.isEqual(trusteeDob) && trustee.fullName == director.fullName
              case _ =>
                false
            }
          }
        )
    }
  }

  private def directors(establisherIndex: Int)(implicit ua: UserAnswers): collection.Seq[IndividualDetails] =
    (ua.data \ EstablishersId.toString \ establisherIndex \ "director").validate[JsArray].asOpt match {
      case Some(jsArray) =>
        jsArray
          .value
          .zipWithIndex
          .flatMap { case (jsValue, directorIndex) =>
            jsValue.validate[IndividualDetails](readsDirector(establisherIndex, directorIndex)) match {
              case JsSuccess(value, _) =>
                Some(value)
              case JsError(errors) =>
                logger.error(
                  "getListOfTrusteesToBeCopied readsDirector failed:" +
                    s"\npath(s) from JSON: ${errors.map(_._1.path.mkString(", "))}" +
                    s"\nerror messages from JSON: ${errors.flatMap(_._2.map(_.messages.head))}"
                )
                None
            }
          }
      case _ =>
        Nil
    }

  def allDirectors(implicit ua: UserAnswers): Seq[IndividualDetails] = {
    ua.data.validate[Seq[Option[Seq[IndividualDetails]]]](readsDirectors) match {
      case JsSuccess(directorsWithEstablishers, _) if directorsWithEstablishers.nonEmpty =>
        directorsWithEstablishers.flatMap(_.toSeq).flatten
      case JsError(errors) =>
        logger.error(
          "readsDirectors failed:" +
            s"\npath(s) from JSON: ${errors.map(_._1.path.mkString(", "))}" +
            s"\nerror messages from JSON: ${errors.flatMap(_._2.map(_.messages.head))}"
        )
        Nil
      case _ =>
        Nil
    }
  }

  private def readsDirectors(implicit ua: UserAnswers): Reads[Seq[Option[Seq[IndividualDetails]]]] =
    new Reads[Seq[Option[Seq[IndividualDetails]]]] {
      private def readsAllDirectors(estIndex: Int)(implicit ua: UserAnswers): Reads[Seq[IndividualDetails]] = {
        case JsArray(directors) =>
          val jsResults: collection.IndexedSeq[JsResult[IndividualDetails]] =
            directors.zipWithIndex.map {
              case (jsValue, dirIndex) => readsDirector(estIndex, dirIndex).reads(jsValue)
            }
          asJsResultSeq(jsResults.toSeq)
        case _ =>
          JsSuccess(Nil)
      }

      override def reads(json: JsValue): JsResult[Seq[Option[Seq[IndividualDetails]]]] = {
        ua.data \ EstablishersId.toString match {
          case JsDefined(JsArray(establishers)) =>
            val jsResults = establishers.zipWithIndex.map {
              case (jsValue, index) =>
                val establisherKind = (jsValue \ EstablisherKindId.toString).validate[String].asOpt

                val readsForEstablisherKind = establisherKind match {

                  case Some(EstablisherKind.Company.toString) =>
                    (JsPath \ "director").readNullable(readsAllDirectors(index))
                  case _ =>
                    Reads.pure[Option[Seq[IndividualDetails]]](None)
                }
                readsForEstablisherKind.reads(jsValue)
            }
            asJsResultSeq(jsResults.toSeq)
          case _ => JsSuccess(Nil)
        }
      }
    }

  private def readsDirector(estIndex: Int, directorIndex: Int)(implicit ua: UserAnswers): Reads[IndividualDetails] =
    (
      (JsPath \ DirectorNameId.toString).read[PersonName] and
      (JsPath \ DirectorDOBId.toString).readNullable[LocalDate] and
      (JsPath \ DirectorNINOId.toString).readNullable[ReferenceValue]
    )(
      (directorName, dob, ninoReferenceVale) =>
        IndividualDetails(
          firstName  = directorName.firstName,
          lastName   = directorName.lastName,
          isDeleted  = directorName.isDeleted,
          nino       = ninoReferenceVale.map(_.value),
          dob        = dob,
          index      = directorIndex,
          isComplete = ua.isDirectorComplete(estIndex, directorIndex),
          mainIndex  = Some(estIndex)
        )
    )

  def allIndividualTrustees(implicit ua: UserAnswers): Seq[IndividualDetails] = {
    ua.data.validate[Seq[Option[IndividualDetails]]](readsTrustees) match {
      case JsSuccess(trustees, _) =>
        trustees.flatten
      case JsError(errors) =>
        logger.error(
          "allIndividualTrustees readsTrustees failed: " +
            s"\npath(s) from JSON: ${errors.map(_._1.path.mkString(", "))}" +
            s"\nerror messages from JSON: ${errors.flatMap(_._2.map(_.messages.head))}"
        )
        Nil
    }
  }

  private def readsIndividualTrustee(index: Int)(implicit ua: UserAnswers): Reads[Option[IndividualDetails]] =
    (
      (JsPath \ TrusteeNameId.toString).read[PersonName] and
      (JsPath \ TrusteeDOBId.toString).readNullable[LocalDate] and
      (JsPath \ TrusteeNINOId.toString).readNullable[ReferenceValue]
    )(
      (trusteeName, dob, ninoReferenceVale) =>
        Some(IndividualDetails(
          firstName  = trusteeName.firstName,
          lastName   = trusteeName.lastName,
          isDeleted  = trusteeName.isDeleted,
          nino       = ninoReferenceVale.map(_.value),
          dob        = dob,
          index      = index,
          isComplete = UserAnswers(
            (ua.data \ TrusteesId.toString).validate[JsArray].asOpt match {
              case Some(jsArray) =>
                Json.obj(TrusteesId.toString -> JsArray(filterTrusteeIndividuals(jsArray)))
              case _ =>
                ua.data
            }
          ).isTrusteeIndividualComplete(index)
        ))
    )

  private def readsTrustees(implicit ua: UserAnswers): Reads[Seq[Option[IndividualDetails]]] =
    (json: JsValue) =>
      (ua.data \ TrusteesId.toString).validate[JsArray].asOpt match {
        case Some(jsArray) =>
          asJsResultSeq(
            filterTrusteeIndividuals(jsArray).zipWithIndex.map {
              case (jsValue, index) =>
                readsIndividualTrustee(index).reads(jsValue)
            }.toSeq
          )
        case _ =>
          JsSuccess(Nil)
      }


  private def asJsResultSeq[A](jsResults: Seq[JsResult[A]]): JsResult[Seq[A]] = {
    JsSuccess(jsResults.collect {
      case JsSuccess(i, _) => i
    })
  }

  private def filterTrusteeIndividuals(jsArray: JsArray): collection.IndexedSeq[JsValue] =
    DataCleanUp.filterNotEmptyObjectsAndSubsetKeys(
      jsArray = jsArray,
      keySet  = Set(IsTrusteeNewId.toString, TrusteeKindId.toString),
      defName = "DataPrefillService.filterTrusteeIndividuals"
    )
    .filter(_
      .\(TrusteeKindId.toString)
      .validate[JsString]
      .asOpt
      .contains(JsString(TrusteeKind.Individual.toString))
    )

}
