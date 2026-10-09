use std::collections::HashMap;

/// Calculates the Shannon entropy of a string: H = -sum(p_i * log2(p_i))
/// Higher values indicate more randomness / obfuscation.
pub fn calculate_shannon_entropy(input: &str) -> f64 {
    if input.is_empty() {
        return 0.0;
    }

    let mut frequencies: HashMap<char, usize> = HashMap::new();
    let mut total_chars = 0;

    for ch in input.chars() {
        *frequencies.entry(ch).or_insert(0) += 1;
        total_chars += 1;
    }

    let mut entropy = 0.0;
    let total_f = total_chars as f64;

    for &count in frequencies.values() {
        let p = (count as f64) / total_f;
        entropy -= p * p.log2();
    }

    entropy
}

/// Evaluates if URL host or path contains suspicious entropy spikes.
/// Natural English hostnames typically have entropy 2.2 - 3.4.
/// DGA (Domain Generation Algorithms) or base64/hex tokens exceed 3.75+.
pub fn evaluate_url_entropy(url_str: &str) -> (bool, f64, String) {
    let clean = url_str
        .trim_start_matches("http://")
        .trim_start_matches("https://");

    let parts: Vec<&str> = clean.splitn(2, '/').collect();
    let host = parts.first().unwrap_or(&"");
    let path = parts.get(1).unwrap_or(&"");

    let host_entropy = calculate_shannon_entropy(host);
    let path_entropy = calculate_shannon_entropy(path);

    if host.len() >= 12 && host_entropy > 3.75 {
        return (
            true,
            host_entropy,
            format!("High host entropy ({:.2}): likely randomized Domain Generation Algorithm (DGA)", host_entropy),
        );
    }

    if path.len() >= 20 && path_entropy > 4.30 {
        return (
            true,
            path_entropy,
            format!("High path/query entropy ({:.2}): obfuscated payload or credential token detected", path_entropy),
        );
    }

    (false, host_entropy.max(path_entropy), String::new())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_entropy_benign() {
        let entropy = calculate_shannon_entropy("google.com");
        assert!(entropy < 3.5);
    }

    #[test]
    fn test_entropy_dga() {
        let dga = "xj92kzm8q47lp9a.top";
        let (flagged, entropy, _) = evaluate_url_entropy(&format!("https://{}", dga));
        assert!(flagged);
        assert!(entropy > 3.75);
    }
}
