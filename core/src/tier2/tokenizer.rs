use std::collections::HashMap;
use std::fs::File;
use std::io::{BufRead, BufReader};
use std::path::Path;

pub struct WordPieceTokenizer {
    vocab: HashMap<String, u32>,
    #[allow(dead_code)]
    inv_vocab: Vec<String>,
    unk_token_id: u32,
    cls_token_id: u32,
    sep_token_id: u32,
    pad_token_id: u32,
}

impl WordPieceTokenizer {
    pub fn new_from_file(vocab_path: &Path) -> Result<Self, String> {
        let file = File::open(vocab_path).map_err(|e| format!("Failed to open vocab file: {}", e))?;
        let reader = BufReader::new(file);

        let mut vocab = HashMap::new();
        let mut inv_vocab = Vec::new();

        for (idx, line) in reader.lines().enumerate() {
            let token = line.map_err(|e| format!("Error reading vocab line: {}", e))?.trim().to_string();
            vocab.insert(token.clone(), idx as u32);
            inv_vocab.push(token);
        }

        Self::from_vocab_map(vocab, inv_vocab)
    }

    pub fn new_embedded() -> Self {
        // Built-in standard WordPiece vocabulary subset for fallback
        let raw_tokens = [
            "[PAD]", "[UNK]", "[CLS]", "[SEP]", "[MASK]",
            "the", "of", "and", "to", "a", "in", "for", "is", "on", "that", "by", "this",
            "http", "https", "www", "com", "net", "top", "xyz", "cfd", "login", "verify",
            "account", "bank", "wire", "transfer", "urgent", "chase", "paypal", "apple",
            "amazon", "security", "alert", "password", "update", "cancel", "fee", "free"
        ];

        let mut vocab = HashMap::new();
        let mut inv_vocab = Vec::new();

        for (idx, &token) in raw_tokens.iter().enumerate() {
            vocab.insert(token.to_string(), idx as u32);
            inv_vocab.push(token.to_string());
        }

        Self::from_vocab_map(vocab, inv_vocab).unwrap()
    }

    fn from_vocab_map(vocab: HashMap<String, u32>, inv_vocab: Vec<String>) -> Result<Self, String> {
        let unk_token_id = *vocab.get("[UNK]").unwrap_or(&1);
        let cls_token_id = *vocab.get("[CLS]").unwrap_or(&2);
        let sep_token_id = *vocab.get("[SEP]").unwrap_or(&3);
        let pad_token_id = *vocab.get("[PAD]").unwrap_or(&0);

        Ok(Self {
            vocab,
            inv_vocab,
            unk_token_id,
            cls_token_id,
            sep_token_id,
            pad_token_id,
        })
    }

    pub fn encode(&self, text: &str, max_len: usize) -> (Vec<u32>, Vec<u32>) {
        let mut token_ids = Vec::with_capacity(max_len);
        let mut attention_mask = Vec::with_capacity(max_len);

        token_ids.push(self.cls_token_id);
        attention_mask.push(1);

        let clean_text = text.to_lowercase();
        let words: Vec<&str> = clean_text.split(|c: char| c.is_whitespace() || c.is_ascii_punctuation()).filter(|w| !w.is_empty()).collect();

        for word in words {
            if token_ids.len() + 1 >= max_len {
                break;
            }

            // WordPiece subword search
            let mut start = 0;
            let mut sub_tokens = Vec::new();
            let chars: Vec<char> = word.chars().collect();

            while start < chars.len() {
                let mut end = chars.len();
                let mut cur_substr = None;

                while start < end {
                    let mut substr: String = chars[start..end].iter().collect();
                    if start > 0 {
                        substr = format!("##{}", substr);
                    }

                    if let Some(&id) = self.vocab.get(&substr) {
                        cur_substr = Some(id);
                        break;
                    }
                    end -= 1;
                }

                if let Some(id) = cur_substr {
                    sub_tokens.push(id);
                    start = end;
                } else {
                    sub_tokens.push(self.unk_token_id);
                    break;
                }
            }

            for id in sub_tokens {
                if token_ids.len() + 1 < max_len {
                    token_ids.push(id);
                    attention_mask.push(1);
                }
            }
        }

        token_ids.push(self.sep_token_id);
        attention_mask.push(1);

        // Pad to max_len
        while token_ids.len() < max_len {
            token_ids.push(self.pad_token_id);
            attention_mask.push(0);
        }

        (token_ids, attention_mask)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_embedded_tokenizer() {
        let tok = WordPieceTokenizer::new_embedded();
        let (ids, mask) = tok.encode("urgent bank wire transfer", 16);
        assert_eq!(ids.len(), 16);
        assert_eq!(mask.len(), 16);
        assert_eq!(ids[0], tok.cls_token_id);
        assert_eq!(mask[0], 1);
    }
}
