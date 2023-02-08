import sbt._

object AppDependencies {

  private val hmrcBootstrapVersion = "7.13.0"

  private val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-28"     % hmrcBootstrapVersion,
    "uk.gov.hmrc"                   %% "play-nunjucks"                  % "0.41.0-play-28",
    "uk.gov.hmrc"                   %% "play-nunjucks-viewmodel"        % "0.17.0-play-28",
    "org.webjars.npm"               %  "govuk-frontend"                 % "4.3.1",
    "org.webjars.npm"               %  "hmrc-frontend"                  % "1.35.2",
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping"  % "1.12.0-play-28",
    "uk.gov.hmrc"                   %% "play-language"                  % "6.1.0-play-28",
    "com.google.inject.extensions"  %  "guice-multibindings"            % "4.2.3",
    "uk.gov.hmrc"                   %% "domain"                         % "8.1.0-play-28",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"           % "2.14.2"
  )


  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % hmrcBootstrapVersion  % Test,
    "org.mockito"             %% "mockito-scala"            % "1.17.5"              % Test,
    "org.jsoup"               % "jsoup"                     % "1.15.3"              % Test,
    "com.vladsch.flexmark"    % "flexmark-all"              % "0.62.2"              % "test, it",
    "org.scalatestplus"       %% "scalacheck-1-17"          % "3.2.15.0"            % Test,
    "com.github.tomakehurst"  % "wiremock-jre8"             % "2.35.0"              % Test,
    "org.pegdown"             % "pegdown"                   % "1.6.0"               % Test
  )

  val all: Seq[ModuleID] = compile ++ test

}
