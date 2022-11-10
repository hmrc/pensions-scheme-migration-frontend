import sbt._

object AppDependencies {

  val hmrcBootstrapVersion = "7.11.0"

  val compile = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-28"     % hmrcBootstrapVersion,
    "uk.gov.hmrc"                   %% "play-nunjucks"                  % "0.40.0-play-28",
    "uk.gov.hmrc"                   %% "play-nunjucks-viewmodel"        % "0.16.0-play-28",
    "org.webjars.npm"               %  "govuk-frontend"                 % "4.3.1",
    "org.webjars.npm"               %  "hmrc-frontend"                  % "1.19.0",
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping"  % "1.12.0-play-28",
    "uk.gov.hmrc"                   %% "play-language"                  % "5.3.0-play-28",
    "com.google.inject.extensions"  %  "guice-multibindings"            % "4.2.3",
    "uk.gov.hmrc"                   %% "domain"                         % "8.1.0-play-28"
  )



  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % hmrcBootstrapVersion % Test,
    "org.mockito" % "mockito-core" % "3.7.7" % Test,
    "org.mockito" %% "mockito-scala" % "1.16.23" % Test,
    "org.mockito" %% "mockito-scala-scalatest" % "1.16.23" % Test,
    "org.jsoup" % "jsoup" % "1.15.3" % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.36.8" % "test, it",
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.14.0" % Test,
    "com.github.tomakehurst" % "wiremock-jre8" % "2.35.0",
    "org.pegdown" % "pegdown" % "1.6.0"
  )




}
