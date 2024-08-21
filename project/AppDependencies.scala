import sbt._

object AppDependencies {

  private val hmrcBootstrapVersion = "8.5.0"
  //TODO: Update play-frontend-hmrc-play-30, bootstrap-frontend-play-30, govuk-frontend after migration to twirl. -Pavel Vjalicin
  //TODO: Remove unused libraries. -Pavel Vjalicin
  private val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"             % hmrcBootstrapVersion,
    "uk.gov.hmrc"                   %% "play-nunjucks-viewmodel-play-30"        % "1.3.0",
    "org.webjars.npm"               %  "govuk-frontend"                         % "4.7.0",
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping-play-30"  % "2.0.0",
    "com.google.inject.extensions"  %  "guice-multibindings"                    % "4.2.3",
    "uk.gov.hmrc"                   %% "domain-play-30"                         % "9.0.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"                   % "2.17.0",
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30"             % "8.5.0"
  )


  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"   % hmrcBootstrapVersion  % Test,
    "org.mockito"             %% "mockito-scala"            % "1.17.31"              % Test,
    "org.jsoup"               % "jsoup"                     % "1.17.2"              % Test,
    "com.vladsch.flexmark"    % "flexmark-all"              % "0.64.8"              % "test, it",
    "org.scalatestplus"       %% "scalacheck-1-17"          % "3.2.18.0"            % Test,
    "org.pegdown"             % "pegdown"                   % "1.6.0"               % Test
  )

  val all: Seq[ModuleID] = compile ++ test

}
