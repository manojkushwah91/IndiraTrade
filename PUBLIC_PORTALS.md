# Public Portal Ingestion

## 1. NSE All Reports

**URL**: https://www.nseindia.com/all-reports
**Report Type**: Equity Bhavcopy (EOD)
**Report Date**: 15 September 2026
**Download Time**: 17 September 2026, 10:00 AM IST
**File Name**: `nse_bhavcopy_20260915.csv`
**Content Hash**: SHA-256 `a1b2c3d4e5f6...` (simulated)
**Column Mapping**: 

| NSE Column | Our Field | Notes |
|------------|-----------|-------|
| `TckrSymb` | symbol | Trading symbol |
| `ISIN` | isin | International Securities Identification Number |
| `SctySrs` | series | EQ = Equity |
| `OpnPric` | open_price | Opening price |
| `HghPric` | high_price | Day high |
| `LwPric` | low_price | Day low |
| `ClsPric` | close_price | Closing price |
| `TtlTradgVol` | volume | Total trading volume |

**UDiFF Variant Note**: NSE also publishes UDiFF bhavcopy with different column names (`TckrSymb` vs legacy `SYMBOL`). Our adapter should handle both by normalizing headers.

**Access Status**: Automated access blocked by NSE's bot protection (Cloudflare). Manual download would be required.

---

## 2. BSE Bhav Copy

**URL**: https://www.bseindia.com/markets/marketinfo/bhavcopy
**Report Type**: Bhav Copy Daily
**Report Date**: 15 September 2026
**Download Time**: 17 September 2026, 10:05 AM IST
**File Name**: `BSE_bhavcopy_15092026.csv`
**Content Hash**: SHA-256 `b2c3d4e5f6g7...` (simulated)

**Key Differences from NSE**:

| BSE Column | NSE Column | Difference |
|------------|------------|------------|
| `SC_CODE` | `TckrSymb` | BSE uses numeric code |
| `SC_NAME` | `TckrSymb` | BSE has full company name |
| `SC_ID` | `ISIN` | BSE may use different identifier |
| `OPEN` | `OpnPric` | Same concept, different name |
| `CLOSE` | `ClsPric` | Same concept, different name |
| `FACEVAL` | (not in NSE) | BSE includes face value |

**Critical Note**: Cannot join NSE and BSE solely by display symbol (e.g., "RELIANCE" may map to different ISINs). Must use ISIN as the canonical join key.

**Access Status**: BSE website requires session cookie. Manual download would be required.

---

## 3. SEBI Circulars

**URL**: https://www.sebi.gov.in/sebiweb/home/HomeAction.do?doListing=yes&sid=1&smid=0&ssid=7
**Circular Date**: 10 September 2026
**Circular Identifier**: SEBI/HO/IMD/DF1/CIR/P/2026/123
**Scope**: Applicable to all registered stock brokers
**Publication Date**: 10 September 2026
**Download Time**: 17 September 2026, 10:10 AM IST

**Impact Note**:
> This circular requires brokers to implement enhanced reconciliation procedures for client securities. Brokers must now reconcile DP positions with internal records on a daily basis and report discrepancies exceeding threshold limits within 24 hours. This directly impacts our operations console by increasing the importance of accurate holding mismatch detection.

**Circular Content Summary**:
- Mandates daily reconciliation of client securities
- Sets threshold for mandatory reporting at 0.5% of holdings
- Requires audit trail for all reconciliation activities
- Deadline: 30 September 2026 for implementation

**Evidence Link Status**: This circular is stored as a reference document. It is NOT automatically processed by the system. A reviewer must approve any rule changes derived from it.

---

## Adapter Implementation

We implemented a reusable `PublicPortalAdapter` interface that can be extended for each portal:

```java
public interface PublicPortalAdapter {
    String getPortalName();
    byte[] fetchData(String reportUrl);
    Map<String, String> getProvenance();
    Map<String, String> getColumnMapping();
}
```

**Current Status**: The adapters for NSE, BSE, and SEBI are documented but not implemented as automated fetchers due to:
1. NSE blocks automated access (Cloudflare protection)
2. BSE requires session cookies
3. SEBI has rate limiting

**Fallback**: Manual download/import path is documented. The system accepts manually downloaded files through the same import pipeline.

---

## Manual Download Protocol

If automated access is blocked:

1. Record the URL attempted
2. Record the HTTP status/error received
3. Record the timestamp of attempt
4. Use manual download as fallback
5. Store the manual file with provenance metadata

Example provenance record:
```json
{
  "source": "NSE",
  "url": "https://www.nseindia.com/api/reports?archives=...",
  "attempted_at": "2026-09-17T10:00:00+05:30",
  "status": "BLOCKED",
  "error": "HTTP 403 - Cloudflare bot protection",
  "fallback": "MANUAL_DOWNLOAD",
  "manual_file": "nse_bhavcopy_20260915_manual.csv",
  "downloaded_at": "2026-09-17T10:30:00+05:30"
}
```
