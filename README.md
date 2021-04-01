# SCOTUS Tracker API
The SCOTUS tracking app is something I put together to help track court cases argued before The Supreme Court of the United States. The actual application can be found [here](https://scotus.jacobhyphenated.com/).

This project holds the back-end API. The [front-end](https://github.com/jacobhyphenated/scotus-app) is a separate application.

This is a work in progress and will change over time.

## What does it do?
Tracks court cases in front of the US Supreme court and keeps a record of older cases.

All data here is manually entered and maintained by me. Links to the actual opinions can be found at the [Supreme Court Website](https://www.supremecourt.gov/opinions/slipopinion). I record a quick summary and paraphrase of the opinions for my own reference.

Why does this even exist? I read Supreme Court opinions for fun. I used to keep track of important cases I was following with a spreadsheet, but writing my own app turned out to be both fun and useful for organizing everything.

## Tech Stack
* **Kotlin** - One reason for doing this project was so that I could write something 100% in Kotlin. Includes **Kotlin Coroutines**.
* **Spring Boot** - This is a REST API using Spring as a framework
* **Spring Data JPA** - To handle the Database of SCOTUS cases
* **Spring Data ElasticSearch** - Text based search
* **Spring RestDocs** - Auto generated API Documentation based on unit tests. If the endpoint gets out of sync with the documentation, the unit tests will fail.

[API Documentation](https://scotus-api.jacobhyphenated.com/docs/index.html)

## Running Locally
You can of course, run the API in your local environment. There are a couple of options for how to do this based on Spring Profiles.

### Local
Use the `local` profile. This mode uses an in memory database with a small amount of pre-populated test data. The data will all reset when the server restarts.

This mode is good for testing changes to the admin section where you may need to create a lot of junk data. This mode is not good for data entry where you want to keep the results.

You need no other external depenencies to run the API in `local` mode.
### Dev
Use the `dev` profile. This still runs in your local environment, but is designed to be more stable. You will need PostgreSQL installed on your device running on port 5423 (the default configuration). Create a database named `scotus` and a user with full access. The username and password should be provided as environment variables named: `SCOTUS_DB_USER` and `SCOTUS_DB_PASS`

To get a head start on creating the database, you can choose to import an outdated set of data from the production environment:
* Create a database named `scotus` and a user named `scotus_local`
  * Grant all privileges to `scotus_local` for the `scotus` database
  * store the username and password as environment variables `SCOTUS_DB_USER` and `SCOTUS_DB_PASS`
* Import the provided sql script `script/db.sql` into the `scotus` database
* Note: The provided data includes an admin user with the username `admin` and the password `password`

### Search
Use of an ElasticSearch environment is controlled by the profile `search`.

ElasticSearch is not necessary to run this application in your local environment, the app will fallback to a wildcard text database lookup if you run without the `search` profile.

You must provide the following environment variables: `ELASTICSEARCH_USER`, `ELASTICSEARCH_PASS`, and `ELASTICSEARCH_URL`. Your ES environment must support SSL and basic authentication.


Note that you can provide multiple active profiles such as `dev,search`

## License
Copyright 2021 Jacob Kanipe-Illig

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.