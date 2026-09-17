package com.indiratrading.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;

public class XlsxBankFileGenerator {

    public static void main(String[] args) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream("src/main/resources/data/bank_confirmation.xlsx")) {

            Sheet sheet = workbook.createSheet("Bank Confirmation");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"client_id", "bank_ref", "transaction_date", "amount_rupees",
                               "direction", "status", "narration"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Data rows
            String[][] data = {
                {"CLI001", "UTR001234567", "2026-09-14", "5000.00", "CREDIT", "COMPLETED",
                 "NEFT transfer from savings account"},
                {"CLI001", "UTR001234567", "2026-09-14", "2000.00", "DEBIT", "COMPLETED",
                 "Equity purchase RELIANCE"},
                {"CLI002", "UTR002345678", "2026-09-15", "7500.00", "CREDIT", "COMPLETED",
                 "NEFT transfer from current account"},
                {"CLI002", "UTR002345678", "2026-09-15", "1000.00", "CREDIT", "PENDING",
                 "NEFT pending confirmation"},
                {"CLI003", "UTR003456789", "2026-09-15", "3000.00", "CREDIT", "COMPLETED",
                 "RTGS transfer"},
                {"CLI003", "UTR004567890", "2026-09-15", "1500.00", "DEBIT", "COMPLETED",
                 "Equity purchase TCS"},
                {"CLI004", "UTR005678901", "2026-09-15", "4000.00", "CREDIT", "COMPLETED",
                 "IMPS transfer"}
            };

            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < data[i].length; j++) {
                    Cell cell = row.createCell(j);
                    if (j == 3) { // amount_rupees - numeric
                        cell.setCellValue(Double.parseDouble(data[i][j]));
                    } else {
                        cell.setCellValue(data[i][j]);
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fos);
            System.out.println("Generated bank_confirmation.xlsx successfully");
        }
    }
}
