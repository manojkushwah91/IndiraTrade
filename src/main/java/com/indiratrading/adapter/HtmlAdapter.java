package com.indiratrading.adapter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class HtmlAdapter implements SourceAdapter {

    @Override
    public String getSourceName() { return "HTML"; }

    @Override
    public String getFileType() { return "HTML"; }

    @Override
    public List<Map<String, String>> parse(byte[] rawData) {
        List<Map<String, String>> records = new ArrayList<>();
        String html = new String(rawData);
        Document doc = Jsoup.parse(html);

        Element table = doc.selectFirst("table");
        if (table == null) return records;

        Elements headers = table.select("thead th");
        List<String> headerList = new ArrayList<>();
        for (Element header : headers) {
            headerList.add(header.text().trim().toLowerCase());
        }

        Elements rows = table.select("tbody tr");
        for (Element row : rows) {
            Map<String, String> record = new HashMap<>();
            Elements cells = row.select("td");
            for (int i = 0; i < headerList.size() && i < cells.size(); i++) {
                record.put(headerList.get(i), cells.get(i).text().trim());
            }
            records.add(record);
        }
        return records;
    }

    @Override
    public Map<String, String> getColumnMapping() {
        return Map.of(
            "client_id", "client_id",
            "isin", "isin",
            "dp_id", "dp_id",
            "settled_qty", "settled_qty",
            "pending_qty", "pending_qty",
            "movement_state", "movement_state",
            "cut_at", "cut_at"
        );
    }

    @Override
    public String getFileExtension() { return ".html"; }

    @Override
    public boolean supports(byte[] rawData) {
        String content = new String(rawData);
        return content.contains("<table") || content.contains("<html");
    }
}
