import sbt._

object AppDependencies {
  import play.core.PlayVersion.current

  val hmrcBootstrapVersion = "5.12.0"

  val compile = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-28"     % hmrcBootstrapVersion,
    "uk.gov.hmrc"                   %% "play-nunjucks"                  % "0.30.0-play-27",
    "uk.gov.hmrc"                   %% "play-nunjucks-viewmodel"        % "0.14.0-play-27",
    "org.webjars.npm"               %  "govuk-frontend"                 % "3.12.0",
    "org.webjars.npm"               %  "hmrc-frontend"                  % "1.19.0",
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping"  % "1.9.0-play-28",
    "uk.gov.hmrc"                   %% "play-language"                  % "5.1.0-play-28",
    "com.google.inject.extensions"  %  "guice-multibindings"            % "4.2.2",
    "uk.gov.hmrc"                   %% "domain"                         % "6.2.0-play-28"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % hmrcBootstrapVersion   % Test,
    "org.mockito"             %  "mockito-core"             % "3.7.7"    % Test,
    "org.mockito"             %% "mockito-scala"            % "1.16.23"  % Test,
    "org.mockito"             %% "mockito-scala-scalatest"  % "1.16.23"  % Test,
    "org.jsoup"               %  "jsoup"                    % "1.13.1"   % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.36.8"   % "test, it",
    "org.scalacheck"          %% "scalacheck"               % "1.15.2",
    "com.github.tomakehurst"  %  "wiremock-jre8"            % "2.21.0",
    "org.pegdown"             %  "pegdown"                  % "1.6.0",
    "org.scalatestplus"       %% "scalatestplus-scalacheck" % "3.1.0.0-RC2"  % Test
  )
}
