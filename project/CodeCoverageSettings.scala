/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {
  val excludedPackages: Seq[String] = Seq(
    "<empty>",
    ".*Reverse.*",
    ".*Routes.*",
    ".*filters.*",
    ".*handlers.*",
    ".*components.*",
    ".*models.*",
    ".*repositories.*",
    ".*BuildInfo.*",
    ".*javascript.*",
    ".*Routes.*",
    ".*GuiceInjector",
    ".*UserAnswersCacheConnector",
    ".*ControllerConfiguration",
    ".*LanguageSwitchController",
    ".*LanguageSelect.*",
    ".*TestMongoPage.*",
    ".*ErrorTemplate.*",
    ".*identifiers*.",
    ".*controllers.testonly*."
  )

  val excludedFiles: Seq[String] = Seq(
    ".*EnterEmailId.*",
    ".*EnterPhoneId.*",
    ".*EstablisherNameId.*",
    ".*CompanyDetailsId.*",
    ".*PartnershipDetailsId.*",
    ".*TrusteeNameId.*",
    ".*EnterPostCodeId.*",
    ".*AddressId.*",
    ".*AddressListId.*",
    ".*AddressYearsId.*",
    ".*EnterPreviousPostCodeId.*",
    ".*PreviousAddressId.*",
    ".*PreviousAddressListId.*",
    ".*HavePAYEId.*",
    ".*HaveUTRId.*",
    ".*HaveVATId.*",
    ".*PartnershipUTRId.*",
    ".*PAYEId.*",
    ".*VATId.*",




  )

  def apply(): Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    ScoverageKeys.coverageExcludedPackages:= excludedPackages.mkString(";"),
    ScoverageKeys.coverageExcludedFiles:= excludedFiles.mkString(";")

  )
}