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

import play.api.Configuration
import play.api.i18n.Lang
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {
  val welshLanguageSupportEnabled: Boolean = config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)
  lazy val locationCanonicalList: String = config.get[String]("location.canonical.list")


  val en: String            = "en"
  val cy: String            = "cy"
  val defaultLanguage: Lang = Lang(en)

  lazy val appName: String = config.get[String](path = "appName")
  lazy val migrationUrl: String = servicesConfig.baseUrl("pensions-scheme-migration")
  lazy val lockUrl: String = s"$migrationUrl${config.get[String](path = "urls.lock")}"
  lazy val lockByUserUrl: String = s"$migrationUrl${config.get[String](path = "urls.lockByUser")}"
  lazy val lockOnSchemeUrl: String = s"$migrationUrl${config.get[String](path = "urls.lockOnScheme")}"
  lazy val dataCacheUrl: String = s"$migrationUrl${config.get[String](path = "urls.dataCache")}"
  lazy val managePensionsSchemeOverviewUrl: String = ""
  lazy val managePensionsSchemeSummaryUrl: String = ""
}
