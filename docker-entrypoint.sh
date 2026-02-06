#!/bin/bash

TRUSTSTORE_FILE="/opt/dataportal-backend/truststore/self-signed-truststore.jks"
TRUSTSTORE_PASS=${TRUSTSTORE_PASS:-changeit}
KEY_PASS=${KEY_PASS:-changeit}

shopt -s nullglob
IFS=$'\n'
ca_files=(certs/*.pem)

if [ ! "${#ca_files[@]}" -eq 0 ]; then

    echo "# At least one CA file with extension *.pem found in certs folder -> starting dataportal backend with own CAs"

    if [[ -f "$TRUSTSTORE_FILE" ]]; then
          echo "## Truststore already exists -> resetting truststore"
          rm "$TRUSTSTORE_FILE"
    fi

    keytool -genkey -alias self-signed-truststore -keyalg RSA -keystore "$TRUSTSTORE_FILE" -storepass "$TRUSTSTORE_PASS" -keypass "$KEY_PASS" -dname "CN=self-signed,OU=self-signed,O=self-signed,L=self-signed,S=self-signed,C=TE" | sed 's/^/          /'
    keytool -delete -alias self-signed-truststore -keystore "$TRUSTSTORE_FILE" -storepass "$TRUSTSTORE_PASS" -noprompt

    tmp_ca_pem_file="$(mktemp)"
    trap 'rm -f "$tmp_ca_pem_file"' EXIT

    for ca_file in ${ca_files[@]}; do
      echo "###"
      echo "### Found PEM file '$ca_file'."
      echo "###   Importing certificates:"

      buf=""
      while IFS= read -r line; do
        buf="$buf$line\n"
        if [ "$line" = "-----END CERTIFICATE-----" ]; then
          printf "%b" "$buf" > "$tmp_ca_pem_file"
          subject="$(openssl x509 -in "$tmp_ca_pem_file" -noout -subject)"
          echo "###     Adding cert: $subject"
          echo "###                  $(openssl x509 -in "$tmp_ca_pem_file" -noout -issuer)"
          keytool -delete -alias "$subject" -keystore "$TRUSTSTORE_FILE" -storepass "$TRUSTSTORE_PASS" -noprompt > /dev/null 2>&1
          keytool -importcert -alias "$subject" -file "$tmp_ca_pem_file" -keystore "$TRUSTSTORE_FILE" -storepass "$TRUSTSTORE_PASS" -noprompt 2>&1 | sed 's/^/###     /'

          buf=""
        fi
      done < "$ca_file"
    done

    echo "### JAVA_OPTS is set to $JAVA_OPTS"

    java $JAVA_OPTS -Djavax.net.ssl.trustStore="$TRUSTSTORE_FILE" -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASS" -jar dataportal-backend.jar
else
    echo "# No CA *.pem cert files found in /opt/dataportal-backend/certs -> starting dataportal backend without own CAs"
    echo "### JAVA_OPTS is set to $JAVA_OPTS"
    java $JAVA_OPTS -jar dataportal-backend.jar
fi

