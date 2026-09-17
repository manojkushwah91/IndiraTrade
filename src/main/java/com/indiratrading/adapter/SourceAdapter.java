package com.indiratrading.adapter;

import java.util.List;
import java.util.Map;

public interface SourceAdapter {
    String getSourceName();
    String getFileType();
    List<Map<String, String>> parse(byte[] rawData);
    Map<String, String> getColumnMapping();
    default String getFileExtension() { return ""; }
    default boolean supports(byte[] rawData) { return true; }
}
