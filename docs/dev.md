# Setting up Development

To run this project, the following steps need to be followed:

1. Add GitHub package repositories
2. Build the project
3. Setup database


## Adding GitHub Package Repositories

This project uses dependencies ([sq2cql](https://github.com/medizininformatik-initiative/sq2cql)) which are not hosted on maven central but on GitHub.

To download artifacts from GitHub package repositories, you need to add your GitHub login credentials to your central maven config file.

For more information, take a look at this GitHub documentation about [authentication](https://docs.github.com/en/free-pro-team@latest/packages/using-github-packages-with-your-projects-ecosystem/configuring-apache-maven-for-use-with-github-packages#authenticating-to-github-packages).

To install the packages using maven in your own projects, you need a personal GitHub access token. This [GitHub documentation](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token) shows you how to generate one.

After that, add the following `<server>` configurations to the `<servers>` section in your local _.m2/settings.xml_. Replace `USERNAME` with your GitHub username and `TOKEN` with the previously generated personal GitHub access token. The token needs at least the scope `read:packages`.

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>mii</id>
      <username>USERNAME</username>
      <password>TOKEN</password>
    </server>
  </servers>
</settings>
```

## Building the Project

Since Release 4.0.0, this project is packaged without ontology files. They have to be downloaded first from the corresponding
[GitHub repository](https://github.com/medizininformatik-initiative/fhir-ontology-generator). This can be done by enabling
the maven profile "download-ontology" when building the project. This wipes any existing ontology files in your project.
So if you are working with your own ontology files, do **not** execute this.

Navigate to the root of this repository and execute `mvn install -Pdownload-ontology` (or omit the -Pdownload-ontology part
when working with your own).

You can change your run configuration in intellij to execute maven goals before running. So if you want to always just
grab the latest ontology from GitHub, you can Edit your run configuration, go to `modify options` and select `add before launch task`
and `run maven goal` with `clean package -Pdownload-ontology`. This is however not necessary, and your mileage may vary
in other IDEs if they offer such an option.

Be aware that Step 1 "Add GitHub package repositories" needs to be executed before.

## Setting up the Database

The project requires a PSQL database. The easiest way to set this up is to use the docker-compose file provided:

`docker-compose up -d`

Note that this starts an empty psql database as well as a containerized version of the backend.
The containerized version of the backend will then connect to the backend database.
One can then connect to the same database when starting the backend in an IDE.
