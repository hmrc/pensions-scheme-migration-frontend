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

package utils

import identifiers.TypedIdentifier
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.director.{DirectorNameId, IsNewDirectorId}
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.establishers.partnership.partner.{IsNewPartnerId, PartnerNameId}
import identifiers.establishers.{EstablisherKindId, EstablishersId, IsEstablisherNewId}
import identifiers.trustees.company.{CompanyDetailsId => TrusteeCompanyDetailsId}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.partnership.{PartnershipDetailsId => TrusteePartnershipDetailsId}
import identifiers.trustees.{IsTrusteeNewId, TrusteeKindId, TrusteesId}
import models._
import models.establishers.EstablisherKind
import models.trustees.TrusteeKind
import play.api.Logger
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import utils.datacompletion.{DataCompletion, DataCompletionEstablishers, DataCompletionTrustees}

import scala.util.{Failure, Success, Try}

final case class UserAnswers(data: JsObject = Json.obj()) extends Enumerable.Implicits with DataCompletion
  with DataCompletionEstablishers with DataCompletionTrustees {

  private val logger = Logger(classOf[UserAnswers])

  def get[A](id: TypedIdentifier[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(id.path)).reads(data).getOrElse(None)

  def get(path: JsPath)(implicit rds: Reads[JsValue]): Option[JsValue] =
    Reads.optionNoError(Reads.at(path)).reads(data).getOrElse(None)

  def getOrException[A](id: TypedIdentifier[A])(implicit rds: Reads[A]): A =
    get(id).getOrElse(throw new RuntimeException("Expected a value but none found for " + id))

  def validate[A](jsValue: JsValue)(implicit rds: Reads[A]): A = {
    jsValue.validate[A].fold(
      invalid =
        errors =>
          throw JsResultException(errors),
      valid =
        response => response
    )
  }

  def set[A](id: TypedIdentifier[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {

    val updatedData = data.setObject(id.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors) =>
        Failure(JsResultException(errors))
    }

    updatedData.map {
      d =>
        val updatedAnswers = copy(data = d)
        id.cleanup(Some(value), updatedAnswers)
    }
  }

  def set(path: JsPath, value: JsValue): Try[UserAnswers] = {

    val updatedData = data.setObject(path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors) =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap {
      d =>
        val updatedAnswers = copy(data = d)
        Success(updatedAnswers)
    }
  }

  def setOrException(path: JsPath, value: JsValue): UserAnswers = set(path, value) match {
    case Success(ua) => ua
    case Failure(ex) => throw ex
  }


  def setOrException[A](id: TypedIdentifier[A], value: A)(implicit writes: Writes[A]): UserAnswers = {
    set(id, value) match {
      case Success(ua) => ua
      case Failure(ex) => throw ex
    }
  }

  def remove(path: JsPath): UserAnswers = {
    data.removeObject(path) match {
      case JsSuccess(jsValue, _) =>
        UserAnswers(jsValue)
      case JsError(_) =>
        throw new RuntimeException("Unable to remove with path: " + path)
    }
  }

  def remove[A](id: TypedIdentifier[A]): UserAnswers = {
    val updatedData = data.removeObject(id.path) match {
      case JsSuccess(jsValue, _) =>
        jsValue
      case JsError(_) =>
        throw new RuntimeException("Unable to remove id: " + id)
    }

    val updatedAnswers = copy(data = updatedData)
    id.cleanup(None, updatedAnswers)
  }

  def removeWithPath(path: JsPath): UserAnswers = {
    data.removeObject(path) match {
      case JsSuccess(jsValue, _) => UserAnswers(jsValue)
      case JsError(_) => throw new RuntimeException("Unable to remove with path: " + path)
    }
  }

  def removeAll(ids: Set[TypedIdentifier[_]]): UserAnswers = {
    @scala.annotation.tailrec
    def removeNext(ids: Set[TypedIdentifier[_]], ua: UserAnswers): UserAnswers = {
      if (ids.isEmpty) {
        ua
      } else {
        removeNext(ids.tail, ua.removeWithPath(ids.head.path))
      }
    }

    removeNext(ids, this)
  }

  def allEstablishersAfterDelete: Seq[Establisher[_]] =
    allEstablishers.filterNot(_.isDeleted)

  def allEstablishers: Seq[Establisher[_]] = {
    data.validate[Seq[Establisher[_]]](readEstablishers) match {
      case JsSuccess(establishers, _) =>
        establishers
      case JsError(errors) =>
        logger.warn(s"Invalid json while reading all the establishers for addEstablisher: $errors")
        Nil
    }
  }


  //scalastyle:off method.length
  def readEstablishers: Reads[Seq[Establisher[_]]] = new Reads[Seq[Establisher[_]]] {

    private def noOfRecords: Int = data.validate((__ \ "establishers").readNullable(__.read(
      Reads.seq((__ \ "establisherKind").read[String].flatMap {
        case "individual" => (__ \ "establisherDetails" \ "isDeleted").json.pick[JsBoolean] orElse notDeleted
        case "company" => (__ \ "companyDetails" \ "isDeleted").json.pick[JsBoolean] orElse notDeleted
        case "partnership" => (__ \ "partnershipDetails" \ "isDeleted").json.pick[JsBoolean] orElse notDeleted
      }).map(_.count(deleted => !deleted.value))))) match {
      case JsSuccess(Some(ele), _) => ele
      case _ => 0
    }

    private def readsIndividual(index: Int): Reads[Establisher[_]] = (
      (JsPath \ EstablisherNameId.toString).read[PersonName] and
        (JsPath \ IsEstablisherNewId.toString).readNullable[Boolean]
      ) ((details, isNew) =>
      EstablisherIndividualEntity(
        EstablisherNameId(index), details.fullName, details.isDeleted,
        isEstablisherIndividualComplete(index), isNew.fold(false)(identity), noOfRecords)
    )

    private def readsCompany(index: Int): Reads[Establisher[_]] = (
      (JsPath \ CompanyDetailsId.toString).read[CompanyDetails] and
        (JsPath \ IsEstablisherNewId.toString).readNullable[Boolean]
      ) ((details, isNew) =>
      EstablisherCompanyEntity(CompanyDetailsId(index),
        details.companyName, details.isDeleted, isEstablisherCompanyAndDirectorsComplete(index), isNew.fold
        (false)(identity), noOfRecords)
    )

    private def readsPartnership(index: Int): Reads[Establisher[_]] = (
      (JsPath \ PartnershipDetailsId.toString).read[PartnershipDetails] and
        (JsPath \ IsEstablisherNewId.toString).readNullable[Boolean]
      ) ((details, isNew) =>
      EstablisherPartnershipEntity(PartnershipDetailsId(index),
        details.partnershipName, details.isDeleted, isEstablisherPartnershipAndPartnersComplete(index), isNew.fold
        (false)(identity), noOfRecords)
    )

    override def reads(json: JsValue): JsResult[Seq[Establisher[_]]] = {
      json \ EstablishersId.toString match {
        case JsDefined(JsArray(establishers)) =>
          val jsResults = establishers.zipWithIndex.map { case (jsValue, index) =>
            val establisherKind = (jsValue \ EstablisherKindId.toString).validate[String].asOpt
            val readsForEstablisherKind = establisherKind match {
              case Some(EstablisherKind.Individual.toString) => readsIndividual(index)
              case Some(EstablisherKind.Company.toString) => readsCompany(index)
              case Some(EstablisherKind.Partnership.toString) => readsPartnership(index)
              case _ => throw UnrecognisedEstablisherKindException
            }
            readsForEstablisherKind.reads(jsValue)
          }
          asJsResultSeq(jsResults.toSeq)
        case _ => JsSuccess(Nil)
      }
    }
  }

  def establishersCount: Int = {
    (data \ EstablishersId.toString).validate[JsArray] match {
      case JsSuccess(establisherArray, _) => establisherArray.value.size
      case _ => 0
    }
  }


  def allTrusteesAfterDelete: Seq[Trustee[_]] =
    allTrustees.filterNot(_.isDeleted)

  def allTrustees: Seq[Trustee[_]] = {
    data.validate[Seq[Trustee[_]]](readTrustees) match {
      case JsSuccess(trustees, _) =>
        trustees
      case JsError(errors) =>
        logger.warn(s"Invalid json while reading all the trustees for addTrustee: $errors")
        Nil
    }
  }

  //scalastyle:off method.length
  def readTrustees: Reads[Seq[Trustee[_]]] = new Reads[Seq[Trustee[_]]] {

    private def noOfRecords: Int = data.validate((__ \ "trustees").readNullable(__.read(
      Reads.seq((__ \ "trusteeKind").read[String].flatMap {
        case "individual" => (__ \ "trusteeDetails" \ "isDeleted").json.pick[JsBoolean] orElse notDeleted
        case "company" => (__ \ "companyDetails" \ "isDeleted").json.pick[JsBoolean] orElse notDeleted
        case "partnership" => (__ \ "partnershipDetails" \ "isDeleted").json.pick[JsBoolean] orElse notDeleted
      }).map(_.count(deleted => !deleted.value))))) match {
      case JsSuccess(Some(ele), _) => ele
      case _ => 0
    }

    private def readsIndividual(index: Int): Reads[Trustee[_]] = (
      (JsPath \ TrusteeNameId.toString).read[PersonName] and
        (JsPath \ IsTrusteeNewId.toString).readNullable[Boolean]
      ) ((details, isNew) =>
      TrusteeIndividualEntity(
        TrusteeNameId(index), details.fullName, details.isDeleted,
        isTrusteeIndividualComplete(index), isNew.fold(false)(identity), noOfRecords)
    )

    private def readsCompany(index: Int): Reads[Trustee[_]] = (
      (JsPath \ TrusteeCompanyDetailsId.toString).read[CompanyDetails] and
        (JsPath \ IsTrusteeNewId.toString).readNullable[Boolean]
      ) ((details, isNew) =>
      TrusteeCompanyEntity(TrusteeCompanyDetailsId(index),
        details.companyName, details.isDeleted, isTrusteeCompanyComplete(index), isNew.fold
        (false)(identity), noOfRecords)
    )

    private def readsPartnership(index: Int): Reads[Trustee[_]] = (
      (JsPath \ TrusteePartnershipDetailsId.toString).read[PartnershipDetails] and
        (JsPath \ IsTrusteeNewId.toString).readNullable[Boolean]
      ) ((details, isNew) =>
      TrusteePartnershipEntity(TrusteePartnershipDetailsId(index),
        details.partnershipName, details.isDeleted, isTrusteePartnershipComplete(index), isNew.fold
        (false)(identity), noOfRecords)
    )

    override def reads(json: JsValue): JsResult[Seq[Trustee[_]]] = {
      json \ TrusteesId.toString match {
        case JsDefined(JsArray(trustees)) =>
          val jsResults = trustees.zipWithIndex.map { case (jsValue, index) =>
            val trusteeKind = (jsValue \ TrusteeKindId.toString).validate[String].asOpt
            val readsForTrusteeKind = trusteeKind match {
              case Some(TrusteeKind.Individual.toString) => readsIndividual(index)
              case Some(TrusteeKind.Company.toString) => readsCompany(index)
              case Some(TrusteeKind.Partnership.toString) => readsPartnership(index)
              case _ => throw UnrecognisedTrusteeKindException
            }
            readsForTrusteeKind.reads(jsValue)
          }

          asJsResultSeq(jsResults.toSeq)
        case _ => JsSuccess(Nil)
      }
    }
  }

  def trusteesCount: Int = {
    (data \ TrusteesId.toString).validate[JsArray] match {
      case JsSuccess(trusteeArray, _) => trusteeArray.value.size
      case _ => 0
    }
  }


  private def notDeleted: Reads[JsBoolean] = __.read(JsBoolean(false))

  private def asJsResultSeq[A](jsResults: Seq[JsResult[A]]): JsResult[Seq[A]] = {
    val allErrors = jsResults.collect {
      case JsError(errors) => errors
    }.flatten

    if (allErrors.nonEmpty) { // If any of JSON is invalid then log warning but return the valid ones
      logger.warn("Errors in JSON: " + allErrors)
    }

    JsSuccess(jsResults.collect {
      case JsSuccess(i, _) => i
    })
  }
  private def traverse[A](seq: Seq[JsResult[A]]): JsResult[Seq[A]] = {
    seq match {
      case s if s.forall(_.isSuccess) =>
        JsSuccess(seq.foldLeft(Seq.empty[A]) {
          case (m, JsSuccess(n, _)) =>
            m :+ n
          case (m, _) =>
            m
        })
      case s =>
        s.collect {
          case e@JsError(_) =>
            e
        }.reduceLeft(JsError.merge)
    }
  }

  def allDirectorsAfterDelete(establisherIndex: Int): Seq[DirectorEntity] = {
    allDirectors(establisherIndex).filterNot(_.isDeleted)
  }

  def getAllRecursive[A](path: JsPath)(implicit rds: Reads[A]): Option[Seq[A]] = {
    JsLens.fromPath(path)
      .getAll(data)
      .flatMap(a => traverse(a.map(Json.fromJson[A]))).asOpt
  }

  def allDirectors(establisherIndex: Int): Seq[DirectorEntity] =
    getAllRecursive[PersonName](DirectorNameId.collectionPath(establisherIndex)).map {
      details =>
        for ((director, directorIndex) <- details.zipWithIndex) yield {
          val isComplete = isDirectorComplete(establisherIndex, directorIndex)
          val isNew = get(IsNewDirectorId(establisherIndex, directorIndex)).getOrElse(false)
          DirectorEntity(
            DirectorNameId(establisherIndex, directorIndex),
            director.fullName,
            director.isDeleted,
            isComplete,
            isNew,
            details.count(!_.isDeleted)
          )
        }
    }.getOrElse(Seq.empty)


def allPartnersAfterDelete(establisherIndex: Int): Seq[PartnerEntity] = {
  allPartners(establisherIndex).filterNot(_.isDeleted)
  }


  def allPartners(establisherIndex: Int): Seq[PartnerEntity] =
    getAllRecursive[PersonName](PartnerNameId.collectionPath(establisherIndex)).map {
      details =>
        for ((partner, partnerIndex) <- details.zipWithIndex) yield {
          val isComplete = isPartnerComplete(establisherIndex, partnerIndex)
          val isNew = get(IsNewPartnerId(establisherIndex, partnerIndex)).getOrElse(false)
          PartnerEntity(
            PartnerNameId(establisherIndex, partnerIndex),
            partner.fullName,
            partner.isDeleted,
            isComplete,
            isNew,
            details.count(!_.isDeleted)
          )
        }
    }.getOrElse(Seq.empty)
  }

case object UnrecognisedEstablisherKindException extends Exception

case object UnrecognisedTrusteeKindException extends Exception


