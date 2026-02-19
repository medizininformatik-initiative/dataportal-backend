# Use

## Support for self-signed certificates

The dataportal backend supports the use of self-signed certificates from your own CAs.
On each startup, the dataportal backend will search through the folder /app/certs inside the container, add all found
CA *.pem files to a java truststore and start the application with this truststore.

Using docker-compose, mount a folder from your host (e.g.: ./certs) to the /app/certs folder,
add your *.pem files (one for each CA you would like to support) to the folder and ensure that they
have the .pem extension.

## Working with the Backend

This backend provides a rest webservice which connects the [Dataportal GUI](https://github.com/medizininformatik-initiative/feasibility-gui)
and the corresponding middlewares.

To send a feasibility query to the backend, use the following example query:

```
curl --location --request POST 'http://localhost:8090/api/v5/query/feasibility' \
--header 'Content-Type: application/json' \
--data-raw '{
    "version": "http://to_be_decided.com/draft-1/schema#",
    "display": "",
    "inclusionCriteria": [
      [
        {
          "termCode": {
            "code": "29463-7",
            "system": "http://loinc.org",
            "version": "v1",
            "display": "Body Weight"
        },
        "valueFilter": {
            "type": "quantity-comparator",
            "unit": {
              "code": "kg",
              "display": "kilogram"
            },
            "comparator": "gt",
            "value": 90
          }
        }
      ]
    ]
  }'
```
another example
```
curl --location --request POST 'http://localhost:8090/api/v5/query/feasibility' \
--header 'Content-Type: application/json' \
--data-raw '{
    "version": "http://to_be_decided.com/draft-1/schema#",
    "display": "xxx",
    "inclusionCriteria": [
      [
        {
          "termCode": {
            "code": "J98.4",
            "system": "urn:oid:1.2.276.0.76.5.409",
            "version": "v1",
            "display": "xxx"
        }

        }
      ]
    ]
  }'
```


The response to this call will return a location header, which links to the endpoint where the result
for the query can be collected with one of the available sub-paths.
For a full description of the api, please refer to the swagger documentation (either in static/v3/api-docs/swagger.yaml
or at http://localhost:8090/api/v5/swagger-ui/index.html when running)


## Starting with Docker

### Creating the Docker Image
```
mvn install
docker build -t dataportal-backend .
```

### Starting the Backend and the Database
```
docker-compose up -d
```

**Note:** _If you need the database to run using another port than 5432 then set the corresponding environment variable like:_
```
DATAPORTAL_DATABASE_PORT=<your-desired-port> docker-compose up -d
```

### Testing if the Container is Running Properly
```
GET http://localhost:8090/api/v5/actuator/health
```

Should reply with status 200 and a JSON object

## Query Result Log Encryption

### Generating a Public/Private Key Pair

According to [BSI TR-02102-1][1], we have to use RSA keys with a minimum size of 3000 bit. We will use 3072 because that is the next possible value.

Generate the private key:

```sh
openssl genrsa -out key.pem 3072
```

Extract the public key from the private key in Base64-encoded DER format to put into `QUERYRESULT_PUBLIC_KEY`:

```sh
openssl rsa -in key.pem -outform DER -pubout | base64
```

If you like to use the `Decryptor` class, you have to convert the private key into the [PKCS#8](https://www.rfc-editor.org/rfc/rfc5208) format:

```sh
openssl pkcs8 -topk8 -inform PEM -outform DER -in key.pem -nocrypt | base64
```

You can use the following Java code to create a `PrivateKey` class for use with `Decryptor`:

```java
var keyFactory = KeyFactory.getInstance("RSA");
var privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode("...")));
```

[1]: <https://www.bsi.bund.de/DE/Themen/Unternehmen-und-Organisationen/Standards-und-Zertifizierung/Technische-Richtlinien/TR-nach-Thema-sortiert/tr02102/tr02102_node.html>
