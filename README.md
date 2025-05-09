# Pensions Scheme Migration Frontend

- [Overview](#overview)
- [Requirements](#requirements)
- [Running the Service](#running-the-service)
- [Enrolments](#enrolments)
- [Compile & Test](#compile--test)
- [Identity Verification Testing](#identity-verification-testing)
- [Navigation](#navigation)
- [Dependencies](#dependencies)
- [Service Documentation](#service-documentation)
- [License](#license)

## Overview

This is the repository for Pension Scheme Migration Frontend. This service allows a user to migrate pension schemes from TPSS including RAC/DACs. All schemes will need to be migrated from TPSS to the MPS infrastructure by end of 2026. RACs (Retired Annuity Contracts) and DACs (Deferred Annuity Contracts) are two older types of pension scheme. A user declares as an administrator of a RAC/DAC. The administrator is responsible for the migration of schemes.

This service has a corresponding back-end microservice to support the migration of legacy schemes and legacy scheme details from TPSS, and registration of legacy schemes to ETMP.

**Associated Backend Link:** https://github.com/hmrc/pensions-scheme-migration

**Stubs:** https://github.com/hmrc/pensions-scheme-stubs

## Requirements
This service is written in Scala and Play, so needs at least a [JRE] to run.

**Node version:** 16.20.2

**Java version:** 21

**Scala version:** 2.13.14

## Running the Service
**Service Manager Profile:** PODS_ALL

**Port:** 8213

**Links:** http://localhost:8213/add-pension-scheme/list-pension-schemes 

http://localhost:8213/add-pension-scheme/rac-dac/add-all 

In order to run the service, ensure Service Manager is installed (see [MDTP guidance](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html) if needed) and launch the relevant configuration by typing into the terminal:
`sm2 --start PODS_ALL`

To run the service locally, enter `sm2 --stop PENSIONS_SCHEME_MIGRATION_FRONTEND`

In your terminal, navigate to the relevant directory and enter `sbt run`.

Access the Authority Wizard and login with the relevant enrolment details [here](http://localhost:9949/auth-login-stub/gg-sign-in)


## Enrolments
There are several different options for enrolling through the auth login stub. In order to enrol as a dummy user to access the platform for local development and testing purposes, the following details must be entered on the auth login page.

For access to the **Pension Administrator dashboard** for local development, enter the following information: 

**Redirect url -** http://localhost:8204/manage-pension-schemes/overview 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key -** HMRC-PODS-ORG 

**Identifier Name -** PsaID 

**Identifier Value -** A2100005

---

To access the **Scheme Registration journey**, enter the following information:

**Redirect URL -** http://localhost:8204/manage-pension-schemes/you-need-to-register 

**GNAP Token -** NO 

**Affinity Group -** Organisation

---

## Compile & Test
**To compile:** Run `sbt compile`

**To test:** Use `sbt test`

**To view test results with coverage:** Run `sbt clean coverage test coverageReport`

For further information on the PODS Test Approach and wider testing including acceptance, accessibility, performance, security and E2E testing, visit the PODS Confluence page [here](https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=PODSP&title=PODS+Test+Approach).

For Journey Tests, visit the [Journey Test Repository](| Journey tests(https://github.com/hmrc/pods-journey-tests).

View the prototype [here](https://pods-event-reporting-prototype.herokuapp.com/).

## Identity verification testing
Additional services required to test IV uplift: KEYSTORE, PLATFORM_ANALYTICS, IV_CALLVALIDATE_PROXY, IV_TEST_DATA, IDENTITY_VERIFICATION_FRONTEND

Relevant application.conf field: urls.iv-uplift-entry

Manual testing might require disabling CORS on identity_verification_frontend repository, this was the case during writing this.

Add the following to application.conf of identity_verification_frontend:
```play.filters.disabled += play.filters.csrf.CSRFFilter```

Eventually we might want to move to iv-stubs, but currently they don't support organisations. identity_verification_stub repository.

## Navigation
The Pension Migration Frontend integrates with the Manage Pension Schemes (MPS) service and uses various stubs available on [GitHub](https://github.com/hmrc/pensions-scheme-stubs). From the Authority Wizard page you will be redirected to the dashboard. Navigate to the migration tile and select 'Add pension schemes registered on the Pension Schemes Online service' to add schemes or 'Add RAC/DACs registered on the Pension Schemes Online service' to add RAC/DACs.

## Dependencies
There are multiple microservices that this service depends on. These are:

| Service                   | Link                                              |
|---------------------------|---------------------------------------------------|
| pensions-scheme-migration | https://github.com/hmrc/pensions-scheme-migration |
| pension-administrator     | https://github.com/hmrc/pension-administrator     |
| address-lookup            | https://github.com/hmrc/address-lookup            |
| email                     | https://github.com/hmrc/email                     |
| auth                      | https://github.com/hmrc/auth                      |
| contact-frontend          | https://github.com/hmrc/contact-frontend          |

## Note on terminology
The terms scheme reference number and submission reference number (SRN) are interchangeable within the PODS codebase; some downstream APIs use scheme reference number, some use submission reference number, probably because of oversight on part of the technical teams who developed these APIs. This detail means the same thing, the reference number that was returned from ETMP when the scheme details were submitted.

## License
This code is open source software Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

[↥ Back to Top](#pensions-scheme-migration-frontend)
