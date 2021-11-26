
# Pensions Scheme Migration Frontend

## Info

This service allows a pensions administrator, either an individual or an organisation, to migrated TPPS Scheme to ETMP.

This service has a corresponding back-end service, namely pensions-scheme-migration.

### Dependencies

|Service                    |Link                                           |
|---------------------------|-----------------------------------------------|
|pensions-scheme-migration  |https://github.com/hmrc/pensions-scheme-migration        |
|pension-administrator      |https://github.com/hmrc/pension-administrator  |
|address-lookup             |https://github.com/hmrc/address-lookup         |
|email                      |https://github.com/hmrc/email                  |
|auth                       |https://github.com/hmrc/auth                   |
|contact-frontend           |https://github.com/hmrc/contact-frontend                   |

### Endpoints used

|Service                    | HTTP Method   | Route | Purpose
|---------------------------|---------------|-------------------------------------------|------------------|
|pensions-scheme-migration  | POST          | /pensions-scheme-migration/register-scheme                    | Register legacy scheme to ETMP |
|pensions-scheme-migration  | GET           | /pensions-scheme-migration/list-of-schemes                    | Returns list of legacy scheme |
|pensions-scheme-migration  | GET           | /pensions-scheme-migration/getLegacySchemeDetails             | Returns details of legacy scheme | 
|pensions-scheme-migration  | POST          | /pensions-scheme-migration/bulk-migration                     | Put Retirement Or Deferred Annuity Contract to Work item for registration with ETMP  | 
|pensions-scheme-migration  | GET           | /pensions-scheme-migration/bulk-migration/isRequestInProgress | Check for Retirement Or Deferred Annuity Contract migration in progress for a PSA | 
|pensions-scheme-migration  | GET           | /pensions-scheme-migration/bulk-migration/isAllFailed         | Check for Retirement Or Deferred Annuity Contract migration failed for a PSA | 
|pensions-scheme-migration  | DELETE        | /pensions-scheme-migration/bulk-migration/deleteAll           | Remove Retirement Or Deferred Annuity Contract migration for a PSA | 
|pension-administrator      | GET           | /pension-administrator/get-email                              | Returns email address for a PSA | 
|pension-administrator      | GET           | /pension-administrator/get-name                               | Returns name of a PSA | 
|pension-administrator      | GET           | /pension-administrator/get-minimal-psa                        | Returns minimal PSA details from DES | 
|address-lookup             | POST          | /lookup                                                       | Returns a list of addresses that match a given postcode | 
|email                      | POST          | /hmrc/email                                                   | Sends an email to an email address |

## Running the service

Service Manager: PODS_ALL

Port: 8213

Link: http://localhost:8213/add-pension-scheme

Enrolment key: HMRC-PODS-ORG

Identifier name: PsaID

Example PSA ID: A2100005

## Tests and prototype

[View the prototype here](https://pods-prototype.herokuapp.com/pages/overview/migration/list-migration)

|Repositories     |Link                                                                   |
|-----------------|-----------------------------------------------------------------------|
|Journey tests    |https://github.com/hmrc/pods-journey-tests       |
|Prototype        |https://pods-prototype.herokuapp.com/pages/overview/migration/list-migration                    |
