import { useMemo } from "react";

export interface ThreatToken {
  text: string;
  weight: number; // 0.00 to 1.00
  source: string; // e.g. "Tier 1: Shannon entropy", "Tier 2: Semantic urgency"
  tier: 1 | 2;
}

export interface ThreatDNAResult {
  tokens: ThreatToken[];
  maxWeight: number;
  dominantTier: 1 | 2;
}

const SUSPICIOUS_KEYWORDS = new Set([
  "login", "verify", "verification", "secure", "security", "account",
  "update", "banking", "bank", "chase", "paypal", "apple", "wellsfargo",
  "urgent", "immediately", "wire", "transfer", "suspended", "password",
  "alert", "action", "auth", "billing", "signin", "wallet", "crypto",
  "confirm", "unauthorized", "irs", "refund", "claim", "prize", "winner"
]);

const SUSPICIOUS_TLDS = new Set([
  "top", "xyz", "tk", "ml", "ga", "cf", "gq", "buzz", "click", "rest", "surf", "icu", "ru"
]);

export function calculateEntropy(str: string): number {
  if (!str) return 0;
  const len = str.length;
  const frequencies = new Map<string, number>();
  for (const char of str) {
    frequencies.set(char, (frequencies.get(char) || 0) + 1);
  }
  let entropy = 0;
  for (const count of frequencies.values()) {
    const p = count / len;
    entropy -= p * Math.log2(p);
  }
  return entropy;
}

export function hasHomoglyph(str: string): boolean {
  // Checks for Cyrillic / Greek or non-basic-Latin characters masquerading as Latin
  return /[\u0400-\u04FF\u0370-\u03FF]/.test(str);
}

export function tokenizePayload(payload: string): ThreatToken[] {
  if (!payload) return [];

  const isUrl = /^https?:\/\//i.test(payload) || payload.includes(".");

  if (isUrl) {
    // Split into structural and lexical chunks
    const tokens: ThreatToken[] = [];
    const urlRegex = /(https?:\/\/)|([a-zA-Z0-9-]+\.)|([a-zA-Z0-9-]+)|([/?&=%#._~:@!$'()*+,;-])|([^a-zA-Z0-9/?&=%#._~:@!$'()*+,;\s]+)/g;
    let match: RegExpExecArray | null;

    while ((match = urlRegex.exec(payload)) !== null) {
      const text = match[0];
      const lower = text.toLowerCase().replace(/[^a-z0-9]/g, "");

      if (hasHomoglyph(text)) {
        tokens.push({
          text,
          weight: 0.98,
          source: "Tier 1: Cyrillic homograph spoofing",
          tier: 1,
        });
      } else if (match[1]) {
        // Protocol
        const isHttp = text.toLowerCase().startsWith("http://");
        tokens.push({
          text,
          weight: isHttp ? 0.45 : 0.08,
          source: isHttp ? "Tier 1: Unencrypted cleartext HTTP transport" : "Tier 1: Standard TLS scheme",
          tier: 1,
        });
      } else if (SUSPICIOUS_KEYWORDS.has(lower)) {
        tokens.push({
          text,
          weight: 0.88,
          source: "Tier 2: Semantic credential harvesting lure",
          tier: 2,
        });
      } else if (SUSPICIOUS_TLDS.has(lower)) {
        tokens.push({
          text,
          weight: 0.84,
          source: "Tier 1: High-risk abused generic TLD",
          tier: 1,
        });
      } else if (text.length >= 6 && calculateEntropy(text) > 3.8) {
        tokens.push({
          text,
          weight: 0.91,
          source: "Tier 1: Shannon entropy randomness anomaly",
          tier: 1,
        });
      } else if (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/.test(text)) {
        tokens.push({
          text,
          weight: 0.92,
          source: "Tier 1: Raw IP host evasion",
          tier: 1,
        });
      } else {
        tokens.push({
          text,
          weight: Math.min(0.25, Math.max(0.04, text.length * 0.015)),
          source: "Tier 2: Baseline neutral token",
          tier: 2,
        });
      }
    }

    return tokens.length > 0 ? tokens : [{ text: payload, weight: 0.1, source: "Tier 2: Neutral payload", tier: 2 }];
  }

  // Natural text / SMS tokenization
  const wordsAndDelims = payload.split(/(\s+|[.,!?;:'"()/\-\\])/);
  return wordsAndDelims
    .filter((chunk) => chunk.length > 0)
    .map((chunk) => {
      const lower = chunk.toLowerCase().trim();
      if (!lower) {
        return {
          text: chunk,
          weight: 0.02,
          source: "Tier 2: Whitespace delimiter",
          tier: 2,
        };
      }

      if (hasHomoglyph(chunk)) {
        return {
          text: chunk,
          weight: 0.99,
          source: "Tier 1: Cyrillic homograph replacement",
          tier: 1,
        };
      }

      if (SUSPICIOUS_KEYWORDS.has(lower)) {
        return {
          text: chunk,
          weight: 0.92,
          source: "Tier 2: Semantic urgency / financial trigger",
          tier: 2,
        };
      }

      if (/https?:\/\/\S+/i.test(chunk)) {
        return {
          text: chunk,
          weight: 0.85,
          source: "Tier 1: Embedded external hyperlink in communication",
          tier: 1,
        };
      }

      if (chunk.length >= 7 && calculateEntropy(chunk) > 3.9) {
        return {
          text: chunk,
          weight: 0.82,
          source: "Tier 1: Shannon entropy character dispersal",
          tier: 1,
        };
      }

      return {
        text: chunk,
        weight: Math.min(0.28, Math.max(0.06, chunk.length * 0.02)),
        source: "Tier 2: Standard natural language token",
        tier: 2,
      };
    });
}

export function useThreatDNA(payload: string): ThreatDNAResult {
  return useMemo(() => {
    const tokens = tokenizePayload(payload);
    let maxWeight = 0;
    let tier1Count = 0;
    let tier2Count = 0;

    for (const t of tokens) {
      if (t.weight > maxWeight) maxWeight = t.weight;
      if (t.tier === 1 && t.weight > 0.4) tier1Count++;
      if (t.tier === 2 && t.weight > 0.4) tier2Count++;
    }

    return {
      tokens,
      maxWeight,
      dominantTier: tier1Count >= tier2Count ? 1 : 2,
    };
  }, [payload]);
}
