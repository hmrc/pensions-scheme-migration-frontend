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

package config

import controllers.routes
import models.ReportTechnicalIssue
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {
  val welshLanguageSupportEnabled: Boolean = config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)

  lazy val locationCanonicalList: String = loadConfig("location.canonical.list.all")
  lazy val locationCanonicalListEUAndEEA: String = loadConfig("location.canonical.list.EUAndEEA")

  val en: String            = "en"
  val cy: String            = "cy"
  val defaultLanguage: Lang = Lang(en)
  private def loadConfig(key: String): String = config.getOptional[String](key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  lazy val appName: String = config.get[String](path = "appName")
  lazy val loginUrl: String = loadConfig("urls.login")
  lazy val govUkLink: String = loadConfig("urls.govUkLink")
  lazy val contactHmrcUrl: String = loadConfig("urls.contactHmrcLink")
  lazy val pensionAdministratorGovUkLink: String = loadConfig("urls.pensionAdministratorGovUkLink")
  lazy val pensionPractitionerGovUkLink: String = loadConfig("urls.pensionPractitionerGovUkLink")

  lazy val registerSchemeAdministratorUrl: String = loadConfig("urls.registerSchemeAdministrator")
  lazy val psaOverviewUrl: String =  loadConfig("urls.psaOverview")
  lazy val migrationUrl: String = servicesConfig.baseUrl("pensions-scheme-migration")
  lazy val lockUrl: String = s"$migrationUrl${config.get[String](path = "urls.lock")}"
  lazy val lockByUserUrl: String = s"$migrationUrl${config.get[String](path = "urls.lockByUser")}"
  lazy val lockOnSchemeUrl: String = s"$migrationUrl${config.get[String](path = "urls.lockOnScheme")}"
  lazy val dataCacheUrl: String = s"$migrationUrl${config.get[String](path = "urls.dataCache")}"
  lazy val schemeDataCacheUrl: String = s"$migrationUrl${config.get[String](path = "urls.schemeDataCache")}"
  lazy val bulkMigrationEnqueueUrl: String = s"$migrationUrl${config.get[String](path = "urls.bulkMigrationEnqueue")}"
  lazy val bulkMigrationIsInProgressUrl: String = s"$migrationUrl${config.get[String](path = "urls.bulkMigrationIsInProgress")}"
  lazy val bulkMigrationIsAllFailedUrl: String = s"$migrationUrl${config.get[String](path = "urls.bulkMigrationIsAllFailed")}"
  lazy val bulkMigrationDeleteAllUrl: String = s"$migrationUrl${config.get[String](path = "urls.bulkMigrationDeleteAll")}"
  def featureToggleUrl(toggle:String) : String = s"$migrationUrl${config.get[String]("urls.featureToggle").format(toggle)}"

  lazy val listOfSchemesUrl: String = s"$migrationUrl${config.get[String](path = "urls.listOfSchemes")}"
  lazy val addressLookUp = s"${servicesConfig.baseUrl("address-lookup")}"
  lazy val yourPensionSchemesUrl: String = loadConfig("urls.yourPensionSchemes")

  lazy val pensionsAdministratorUrl = s"${servicesConfig.baseUrl("pension-administrator")}"
  lazy val getPSAEmail: String = s"$pensionsAdministratorUrl${config.get[String]("urls.get-psa-email")}"
  lazy val getPSAName: String = s"$pensionsAdministratorUrl${config.get[String]("urls.get-psa-name")}"
  lazy val getPSAMinDetails: String = s"$pensionsAdministratorUrl${config.get[String]("urls.get-psa-min-details")}"

  lazy val psaUpdateContactDetailsUrl: String = loadConfig("urls.psaUpdateContactDetails")
  lazy val deceasedContactHmrcUrl: String = loadConfig("urls.deceasedContactHmrc")
  lazy val psaDelimitedUrl: String = loadConfig("urls.psaDelimited")

  lazy val schemesMigrationViewOnly: String = config.get[String]("urls.schemes-migration-view-only")
  lazy val racDacMigrationViewOnly: String = config.get[String]("urls.rac-dacs-migration-view-only")
  lazy val schemesMigrationTransfer: String = config.get[String]("urls.schemes-migration-transfer")
  lazy val racDacMigrationTransfer: String = config.get[String]("urls.rac-dacs-migration-transfer")
  lazy val racDacMigrationCheckStatus: String = config.get[String]("urls.rac-dacs-migration-check-status")

  val reportAProblemPartialUrl: String = getConfigString("contact-frontend.report-problem-url.with-js")
  val reportAProblemNonJSUrl: String = getConfigString("contact-frontend.report-problem-url.non-js")
  val betaFeedbackUrl: String = getConfigString("contact-frontend.beta-feedback-url.authenticated")
  val betaFeedbackUnauthenticatedUrl: String = getConfigString("contact-frontend.beta-feedback-url.unauthenticated")
  val reportTechnicalIssues: ReportTechnicalIssue = ReportTechnicalIssue(serviceId = "PODS", baseUrl = Some(reportAProblemNonJSUrl))
  lazy val languageTranslationEnabled: Boolean = config.getOptional[Boolean]("features" +
    ".welsh-translation").getOrElse(false)
  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))

  def routeToSwitchLanguage: String => Call = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)

  private def getConfigString(key: String) = servicesConfig.getConfString(key, throw new Exception(s"Could not find " +
    s"config '$key'"))

  lazy val managePensionsSchemeSummaryUrl: String = "Test"
  lazy val serviceSignOut: String = s"${config.get[String](path = "urls.logout")}"
  lazy val timeoutSeconds: String = s"${config.get[String](path = "session.timeoutSeconds")}"
  lazy val CountdownInSeconds: String = s"${config.get[String](path = "session.CountdownInSeconds")}"
  lazy val validCountryCodes: Seq[String] = s"${config.get[String](path = "validCountryCodes")}".split(",").toSeq
  lazy val maxDirectors: Int = loadConfig("company.maxDirectors").toInt
  lazy val maxTrustees: Int = loadConfig("company.maxTrustees").toInt
  lazy val maxPartners: Int = loadConfig("maxPartners").toInt
  lazy val listSchemePagination: Int = loadConfig("listSchemePagination").toInt
  lazy val emailApiUrl: String = s"${servicesConfig.baseUrl("email")}"
  lazy val schemeConfirmationEmailTemplateId: String = loadConfig("email.schemeConfirmationTemplateId")
  lazy val bulkMigrationConfirmationEmailTemplateId: String = loadConfig("email.bulkMigrationConfirmationTemplateId")
  lazy val emailSendForce: Boolean = config.getOptional[Boolean]("email.force").getOrElse(false)
}
