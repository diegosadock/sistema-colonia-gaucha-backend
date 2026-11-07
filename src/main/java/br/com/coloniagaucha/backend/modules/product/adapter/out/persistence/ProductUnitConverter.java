package br.com.coloniagaucha.backend.modules.product.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.product.domain.UnitOfMeasure;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductUnitConverter implements AttributeConverter<UnitOfMeasure, String> {
    @Override public String convertToDatabaseColumn(UnitOfMeasure unit) { return unit == null ? null : unit.code(); }
    @Override public UnitOfMeasure convertToEntityAttribute(String value) { return value == null ? null : UnitOfMeasure.fromCode(value); }
}
