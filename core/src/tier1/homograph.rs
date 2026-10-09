/// Cyrillic & Confusable Homograph Detector
/// Detects IDN spoofing, punycode attacks, and mixed-script Cyrillic/Greek character injection.

pub struct HomographFinding {
    pub is_threat: bool,
    pub reason: String,
    pub decoded_preview: String,
}

pub fn detect_homograph(input: &str) -> HomographFinding {
    // 1. Check for Punycode prefix
    if input.contains("xn--") {
        return HomographFinding {
            is_threat: true,
            reason: "Punycode domain detected (xn--): internationalized domain disguising non-Latin characters".to_string(),
            decoded_preview: input.to_string(),
        };
    }

    // 2. Scan characters for Cyrillic / Greek confusables mixed into Latin text
    let mut cyrillic_confusables = Vec::new();
    let mut has_latin = false;
    let mut has_cyrillic = false;

    for ch in input.chars() {
        if ch.is_ascii_alphabetic() {
            has_latin = true;
        } else if is_cyrillic_confusable(ch) {
            has_cyrillic = true;
            cyrillic_confusables.push(ch);
        }
    }

    if has_cyrillic && has_latin {
        let sample = cyrillic_confusables.iter().take(3).collect::<String>();
        return HomographFinding {
            is_threat: true,
            reason: format!(
                "Cyrillic homograph detected: mixed-script spoofing using characters [{}] to mimic standard Latin characters",
                sample
            ),
            decoded_preview: format!("Confusable characters: {:?}", cyrillic_confusables),
        };
    }

    HomographFinding {
        is_threat: false,
        reason: String::new(),
        decoded_preview: String::new(),
    }
}

fn is_cyrillic_confusable(ch: char) -> bool {
    matches!(
        ch,
        '\u{0430}' /* а */ |
        '\u{0441}' /* с */ |
        '\u{0435}' /* е */ |
        '\u{043E}' /* о */ |
        '\u{0440}' /* р */ |
        '\u{0445}' /* х */ |
        '\u{0443}' /* у */ |
        '\u{0456}' /* і */ |
        '\u{0458}' /* ј */ |
        '\u{0455}' /* ѕ */ |
        '\u{0410}' /* А */ |
        '\u{0412}' /* В */ |
        '\u{0421}' /* С */ |
        '\u{0415}' /* Е */ |
        '\u{041D}' /* Н */ |
        '\u{041E}' /* О */ |
        '\u{0420}' /* Р */ |
        '\u{0422}' /* Т */ |
        '\u{0425}' /* Х */
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_benign_latin_domain() {
        let finding = detect_homograph("https://paypal.com/signin");
        assert!(!finding.is_threat);
    }

    #[test]
    fn test_cyrillic_spoofed_paypal() {
        // 'р' is Cyrillic \u{0440}
        let finding = detect_homograph("https://\u{0440}aypal.com/verify");
        assert!(finding.is_threat);
        assert!(finding.reason.contains("Cyrillic homograph"));
    }

    #[test]
    fn test_punycode_domain() {
        let finding = detect_homograph("https://xn--e1afmkfd.xn--p1ai");
        assert!(finding.is_threat);
        assert!(finding.reason.contains("Punycode"));
    }
}
