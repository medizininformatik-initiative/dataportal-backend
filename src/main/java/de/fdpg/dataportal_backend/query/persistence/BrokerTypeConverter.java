package de.fdpg.dataportal_backend.query.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BrokerTypeConverter implements AttributeConverter<BrokerClientType, String> {

  @Override
  public String convertToDatabaseColumn(BrokerClientType brokerClientType) {
    if (brokerClientType == null) {
      return null;
    }

    return brokerClientType.name();
  }

  @Override
  public BrokerClientType convertToEntityAttribute(String value) {
    if (value == null) {
      return null;
    }

    return BrokerClientType.valueOf(value);
  }
}
