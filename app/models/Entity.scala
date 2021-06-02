package models

sealed trait Entity[ID] {
  def id: ID

  def name: String

  def isDeleted: Boolean

  def isCompleted: Boolean

  def editLink(mode: Mode, srn: Option[String]): Option[String]

  def deleteLink(mode: Mode, srn: Option[String]): Option[String]

  def index: Int
}

sealed trait Establisher[T] extends Entity[T]

case class EstablisherIndividualEntity(id: EstablisherNameId, name: String, isDeleted: Boolean,
                                       isCompleted: Boolean, isNewEntity: Boolean, noOfRecords: Int) extends
  Establisher[EstablisherNameId] {
  override def editLink(mode: Mode, srn: Option[String]): Option[String] = None

  override def deleteLink(mode: Mode, srn: Option[String]): Option[String] = {
    mode match {
      case NormalMode | CheckMode =>
        Some(controllers.register.establishers.routes.ConfirmDeleteEstablisherController.onPageLoad(mode, id.index,
          EstablisherKind.Indivdual, srn).url)
      case UpdateMode | CheckUpdateMode if noOfRecords > 1 =>
        Some(controllers.register.establishers.routes.ConfirmDeleteEstablisherController.onPageLoad(mode, id.index,
          EstablisherKind.Indivdual, srn).url)
      case _ => None
    }
  }

  override def index: Int = id.index
}