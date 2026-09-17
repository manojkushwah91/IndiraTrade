package com.indiratrading.parser;

import java.util.List;
import java.util.Map;

public interface DataParser<T> {
    List<T> parse(byte[] rawData);
    String getSourceName();
    Map<String, String> getColumnMapping();
}
