import sbt._

object AppDependencies {

  private val hmrcBootstrapVersion = "8.4.0"

  private val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"             % hmrcBootstrapVersion,
    "uk.gov.hmrc"                   %% "play-nunjucks-viewmodel-play-30"        % "1.0.0",
    "org.webjars.npm"               %  "govuk-frontend"                         % "4.7.0",
    "org.webjars.npm"               %  "hmrc-frontend"                          % "1.35.2",
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping-play-30"  % "2.0.0",
    "uk.gov.hmrc"                   %% "play-language-play-30"                  % "7.0.0",
    "com.google.inject.extensions"  %  "guice-multibindings"                    % "4.2.3",
    "uk.gov.hmrc"                   %% "domain-play-30"                         % "9.0.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"                   % "2.17.0"
  )


  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"   % hmrcBootstrapVersion  % Test,
    "org.mockito"             %% "mockito-scala"            % "1.17.30"              % Test,
    "org.jsoup"               % "jsoup"                     % "1.17.2"              % Test,
    "com.vladsch.flexmark"    % "flexmark-all"              % "0.64.6"              % "test, it",
    "org.scalatestplus"       %% "scalacheck-1-17"          % "3.2.17.0"            % Test,
    "org.pegdown"             % "pegdown"                   % "1.6.0"               % Test
  )

  val all: Seq[ModuleID] = compile ++ test

}
