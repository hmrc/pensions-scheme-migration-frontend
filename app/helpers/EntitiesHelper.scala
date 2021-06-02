package helpers

import models.{CompanyDetails, Mode, PartnershipDetails}
import play.api.libs.json.{JsArray, JsBoolean, JsDefined, JsError, JsPath, JsResult, JsSuccess, JsValue, Reads, __}


class EntitiesHelper {

  sealed trait Establisher[T] extends Entity[T]

  def allEstablishersAfterDelete(mode: Mode): Seq[Establisher[_]] =
    allEstablishers(mode).filterNot(_.isDeleted)

  def allEstablishers(mode: Mode): Seq[Establisher[_]] = {
    json.validate[Seq[Establisher[_]]](readEstablishers(mode)) match {
      case JsSuccess(establishers, _) =>
        establishers
      case JsError(errors) =>
        logger.warn(s"Invalid json while reading all the establishers for addEstablisher: $errors")
        Nil
    }
  }

  //scalastyle:off method.length
  def readEstablishers(mode: Mode): Reads[Seq[Establisher[_]]] = new Reads[Seq[Establisher[_]]] {

    private def noOfRecords: Int = json.validate((__ \ 'establishers).readNullable(__.read(
      Reads.seq((__ \ 'establisherKind).read[String].flatMap {
        case "individual" => (__ \ 'establisherDetails \ "isDeleted").json.pick[JsBoolean] orElse notDeleted
        case "company" => (__ \ 'companyDetails \ "isDeleted").json.pick[JsBoolean] orElse notDeleted
        case "partnership" => (__ \ 'partnershipDetails \ "isDeleted").json.pick[JsBoolean] orElse notDeleted
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
      (JsPath \ EstablisherCompanyDetailsId.toString).read[CompanyDetails] and
        (JsPath \ IsEstablisherNewId.toString).readNullable[Boolean]
      ) ((details, isNew) =>
      EstablisherCompanyEntity(EstablisherCompanyDetailsId(index),
        details.companyName, details.isDeleted, isEstablisherCompanyAndDirectorsComplete(index, mode), isNew.fold
        (false)(identity), noOfRecords)
    )

    private def readsPartnership(index: Int): Reads[Establisher[_]] = (
      (JsPath \ PartnershipDetailsId.toString).read[PartnershipDetails] and
        (JsPath \ IsEstablisherNewId.toString).readNullable[Boolean]
      ) ((details, isNew) =>
      EstablisherPartnershipEntity(PartnershipDetailsId(index),
        details.name, details.isDeleted, isEstablisherPartnershipAndPartnersComplete(index), isNew.fold(false)
        (identity), noOfRecords)
    )

    private def readsSkeleton(index: Int): Reads[Establisher[_]] = new Reads[Establisher[_]] {
      override def reads(json: JsValue): JsResult[Establisher[_]] = {
        (json \ EstablisherKindId.toString)
          .toOption.map(_ => JsSuccess(EstablisherSkeletonEntity(EstablisherKindId(index))))
          .getOrElse(JsError(s"Establisher does not have element establisherKind: index=$index"))
      }
    }

    override def reads(json: JsValue): JsResult[Seq[Establisher[_]]] = {
      json \ EstablishersId.toString match {
        case JsDefined(JsArray(establishers)) =>
          val jsResults = establishers.zipWithIndex.map { case (jsValue, index) =>
            val establisherKind = (jsValue \ EstablisherKindId.toString).validate[String].asOpt
            val readsForEstablisherKind = establisherKind match {
              case Some(EstablisherKind.Indivdual.toString) => readsIndividual(index)
              case Some(EstablisherKind.Company.toString) => readsCompany(index)
              case Some(EstablisherKind.Partnership.toString) => readsPartnership(index)
              case _ => readsSkeleton(index)
            }
            readsForEstablisherKind.reads(jsValue)
          }

          asJsResultSeq(jsResults)
        case _ => JsSuccess(Nil)
      }
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




}
