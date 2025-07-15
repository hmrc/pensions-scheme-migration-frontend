import sbt.*

object AppDependencies {

  private val hmrcBootstrapVersion = "9.14.0"
  private val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"             % hmrcBootstrapVersion,
    "org.webjars.npm"               %  "govuk-frontend"                         % "5.10.0",
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping-play-30"  % "3.3.0",
    "com.google.inject.extensions"  %  "guice-multibindings"                    % "4.2.3",
    "uk.gov.hmrc"                   %% "domain-play-30"                         % "12.1.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"                   % "2.19.0",
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30"             % "12.7.0"
  )


  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"   % hmrcBootstrapVersion  % Test,
    "org.scalatestplus" %% "mockito-4-11"             % "3.2.18.0"            % Test,
    "org.scalatestplus" %% "scalacheck-1-18"          % "3.2.19.0"            % Test
  )

  val all: Seq[ModuleID] = compile ++ test

}
