package com.indiratrading.adapter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.*;

@Component
public class XlsxAdapter implements SourceAdapter {

    @Override
    public String getSourceName() { return "XLSX"; }

    @Override
    public String getFileType() { return "XLSX"; }

    @Override
    public List<Map<String, String>> parse(byte[] rawData) {
        List<Map<String, String>> records = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(rawData))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return records;

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) return records;

            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell).trim().toLowerCase());
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> record = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i);
                    record.put(headers.get(i), cell != null ? getCellValue(cell).trim() : "");
                }
                records.add(record);
            }
        } catch (Exception e) {
            throw new RuntimeException("XLSX parsing failed: " + e.getMessage(), e);
        }
        return records;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    return String.valueOf((long) val);
                }
                return String.valueOf(val);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }

    @Override
    public Map<String, String> getColumnMapping() {
        return Map.of(
            "client_id", "client_id",
            "bank_ref", "bank_ref",
            "transaction_date", "transaction_date",
            "amount_rupees", "amount_rupees",
            "direction", "direction",
            "status", "status",
            "narration", "narration"
        );
    }

    @Override
    public String getFileExtension() { return ".xlsx"; }
}
