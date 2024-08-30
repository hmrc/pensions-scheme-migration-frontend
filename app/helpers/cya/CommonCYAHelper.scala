package helpers.cya

import models.entities
import models.entities.{EntityRepresentetive, EntityType, JourneyType, PensionManagementType}

import javax.inject.{Inject, Singleton}

@Singleton
class CommonCYAHelper @Inject() () {
  def rows(pensionManagementType: PensionManagementType,
           entityType: EntityType,
           entityRepresentetive: Option[EntityRepresentetive],
           journeyType: JourneyType) = {

    journeyType match {
      case entities.Address => ???
      case entities.Contacts => ???
      case entities.Details => ???
      case _ => ???
    }
  }
}
