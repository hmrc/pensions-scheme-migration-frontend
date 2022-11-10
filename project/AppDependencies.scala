import sbt._

object AppDependencies {

  private val hmrcBootstrapVersion = "7.11.0"

  private val jacksonVersion = "2.13.2"
  private val jacksonDatabindVersion = "2.13.2.2"

  private val jacksonOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core" % "jackson-core",
    "com.fasterxml.jackson.core" % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion)

  private val jacksonDatabindOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion
  )

  private val akkaSerializationJacksonOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
    "com.fasterxml.jackson.module" % "jackson-module-parameter-names",
    "com.fasterxml.jackson.module" %% "jackson-module-scala",
  ).map(_ % jacksonVersion)

  private val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-28"     % hmrcBootstrapVersion,
        "uk.gov.hmrc" %% "play-nunjucks" % "0.40.0-play-28",
        "uk.gov.hmrc" %% "play-nunjucks-viewmodel" % "0.16.0-play-28",
    "org.webjars.npm"               %  "govuk-frontend"                 % "4.2.0",
    "org.webjars.npm"               %  "hmrc-frontend"                  % "1.35.2",
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping"  % "1.12.0-play-28",
    "uk.gov.hmrc"                   %% "play-language"                  % "5.3.0-play-28",
    "com.google.inject.extensions"  %  "guice-multibindings"            % "4.2.3",
    "uk.gov.hmrc"                   %% "domain"                         % "8.1.0-play-28"
//    "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.12.4"            % Test
  )


  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % hmrcBootstrapVersion % Test,
    "org.mockito"         %% "mockito-scala"           %   "1.17.5" % Test,
    "org.jsoup" % "jsoup" % "1.15.3" % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % "test, it",
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.14.0" % Test,
    "com.github.tomakehurst" % "wiremock-jre8" % "2.35.0",
    "org.pegdown" % "pegdown" % "1.6.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.14.0"            % Test
  )

  val all: Seq[ModuleID] = compile ++ jacksonDatabindOverrides ++ jacksonOverrides ++ akkaSerializationJacksonOverrides ++ test

}
