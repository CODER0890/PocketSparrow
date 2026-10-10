// Pure on-device forensic archive generator and AES-256-GCM encryption engine

export interface ForensicReportData {
  metadata: Record<string, any>;
  threatDna: Record<string, any>;
  networkLog: string;
  sla: Record<string, any>;
}

// Simple CRC32 table implementation
const makeCrcTable = (): Uint32Array => {
  const table = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) {
      c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
    table[n] = c >>> 0;
  }
  return table;
};

const CRC_TABLE = makeCrcTable();

export function crc32(buf: Uint8Array): number {
  let crc = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    crc = CRC_TABLE[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

export function createZipArchive(files: { name: string; content: string | Uint8Array }[]): Uint8Array {
  const encoder = new TextEncoder();
  const fileRecords: {
    nameBytes: Uint8Array;
    data: Uint8Array;
    crc: number;
    offset: number;
  }[] = [];

  let currentOffset = 0;
  const localChunks: Uint8Array[] = [];

  for (const file of files) {
    const nameBytes = encoder.encode(file.name);
    const data = typeof file.content === "string" ? encoder.encode(file.content) : file.content;
    const fileCrc = crc32(data);

    // Local file header (30 bytes + name length)
    const header = new Uint8Array(30 + nameBytes.length);
    const view = new DataView(header.buffer);
    view.setUint32(0, 0x04034b50, true); // Local file header signature
    view.setUint16(4, 20, true); // Version needed to extract (2.0)
    view.setUint16(6, 0, true); // General purpose bit flag
    view.setUint16(8, 0, true); // Compression method (0 = stored)
    view.setUint16(10, 0, true); // Mod time
    view.setUint16(12, 0, true); // Mod date
    view.setUint32(14, fileCrc, true); // CRC-32
    view.setUint32(18, data.length, true); // Compressed size
    view.setUint32(22, data.length, true); // Uncompressed size
    view.setUint16(26, nameBytes.length, true); // File name length
    view.setUint16(28, 0, true); // Extra field length
    header.set(nameBytes, 30);

    fileRecords.push({
      nameBytes,
      data,
      crc: fileCrc,
      offset: currentOffset,
    });

    localChunks.push(header);
    localChunks.push(data);
    currentOffset += header.length + data.length;
  }

  // Central directory
  const cdOffset = currentOffset;
  const cdChunks: Uint8Array[] = [];
  let cdSize = 0;

  for (const record of fileRecords) {
    const cdHeader = new Uint8Array(46 + record.nameBytes.length);
    const view = new DataView(cdHeader.buffer);
    view.setUint32(0, 0x02014b50, true); // Central file header signature
    view.setUint16(4, 20, true); // Version made by
    view.setUint16(6, 20, true); // Version needed to extract
    view.setUint16(8, 0, true); // General purpose bit flag
    view.setUint16(10, 0, true); // Compression method
    view.setUint16(12, 0, true); // Mod time
    view.setUint16(14, 0, true); // Mod date
    view.setUint32(16, record.crc, true); // CRC-32
    view.setUint32(20, record.data.length, true); // Compressed size
    view.setUint32(24, record.data.length, true); // Uncompressed size
    view.setUint16(28, record.nameBytes.length, true); // File name length
    view.setUint16(30, 0, true); // Extra field length
    view.setUint16(32, 0, true); // File comment length
    view.setUint16(34, 0, true); // Disk number start
    view.setUint16(36, 0, true); // Internal attributes
    view.setUint32(38, 0, true); // External attributes
    view.setUint32(42, record.offset, true); // Relative offset of local header
    cdHeader.set(record.nameBytes, 46);

    cdChunks.push(cdHeader);
    cdSize += cdHeader.length;
  }

  // End of central directory record (22 bytes)
  const eocd = new Uint8Array(22);
  const eocdView = new DataView(eocd.buffer);
  eocdView.setUint32(0, 0x06054b50, true); // EOCD signature
  eocdView.setUint16(4, 0, true); // Number of this disk
  eocdView.setUint16(6, 0, true); // Disk where central directory starts
  eocdView.setUint16(8, fileRecords.length, true); // Number of central directory records on this disk
  eocdView.setUint16(10, fileRecords.length, true); // Total number of central directory records
  eocdView.setUint32(12, cdSize, true); // Size of central directory
  eocdView.setUint32(16, cdOffset, true); // Offset of start of central directory
  eocdView.setUint16(20, 0, true); // Comment length

  const totalLength = currentOffset + cdSize + eocd.length;
  const result = new Uint8Array(totalLength);
  let pos = 0;

  for (const chunk of localChunks) {
    result.set(chunk, pos);
    pos += chunk.length;
  }
  for (const chunk of cdChunks) {
    result.set(chunk, pos);
    pos += chunk.length;
  }
  result.set(eocd, pos);

  return result;
}

/**
 * Encrypts raw data with AES-256-GCM using a PBKDF2-derived key (100,000 iterations).
 * Returns the salt, IV, and ciphertext formatted with SHA-256 digest.
 */
export async function encryptForensicArchive(
  data: Uint8Array,
  password: string
): Promise<{ encryptedBlob: Blob; sha256Hex: string }> {
  const enc = new TextEncoder();
  const passwordBytes = enc.encode(password);

  // 1. Derive key via PBKDF2
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iv = crypto.getRandomValues(new Uint8Array(12));

  const baseKey = await crypto.subtle.importKey(
    "raw",
    passwordBytes,
    { name: "PBKDF2" },
    false,
    ["deriveKey"]
  );

  const aesKey = await crypto.subtle.deriveKey(
    {
      name: "PBKDF2",
      salt,
      iterations: 100000,
      hash: "SHA-256",
    },
    baseKey,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt"]
  );

  // 2. Encrypt with AES-GCM
  const ciphertextBuffer = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv },
    aesKey,
    data as unknown as BufferSource
  );
  const ciphertext = new Uint8Array(ciphertextBuffer);

  // 3. Package output: [SPAR prefix 4B][Salt 16B][IV 12B][Ciphertext]
  const magic = new Uint8Array([0x53, 0x50, 0x41, 0x52]); // "SPAR"
  const output = new Uint8Array(magic.length + salt.length + iv.length + ciphertext.length);
  output.set(magic, 0);
  output.set(salt, magic.length);
  output.set(iv, magic.length + salt.length);
  output.set(ciphertext, magic.length + salt.length + iv.length);

  // 4. Calculate SHA-256 hash of final output
  const hashBuffer = await crypto.subtle.digest("SHA-256", output as unknown as BufferSource);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const sha256Hex = hashArray.map((b) => b.toString(16).padStart(2, "0")).join("");

  const encryptedBlob = new Blob([output], { type: "application/octet-stream" });
  return { encryptedBlob, sha256Hex };
}

/**
 * Calculates offline password strength entropy (0 - 100 score).
 */
export function calculatePasswordStrength(password: string): {
  score: number; // 0 - 100
  label: "Very Weak" | "Weak" | "Fair" | "Strong" | "Military Grade";
  checks: {
    hasLength: boolean;
    hasUpper: boolean;
    hasLower: boolean;
    hasNumber: boolean;
    hasSymbol: boolean;
  };
} {
  const checks = {
    hasLength: password.length >= 10,
    hasUpper: /[A-Z]/.test(password),
    hasLower: /[a-z]/.test(password),
    hasNumber: /[0-9]/.test(password),
    hasSymbol: /[^A-Za-z0-9]/.test(password),
  };

  let score = 0;
  if (password.length >= 8) score += 20;
  if (password.length >= 12) score += 15;
  if (password.length >= 16) score += 15;
  if (checks.hasUpper) score += 10;
  if (checks.hasLower) score += 10;
  if (checks.hasNumber) score += 15;
  if (checks.hasSymbol) score += 15;

  let label: "Very Weak" | "Weak" | "Fair" | "Strong" | "Military Grade" = "Very Weak";
  if (score >= 85) label = "Military Grade";
  else if (score >= 70) label = "Strong";
  else if (score >= 50) label = "Fair";
  else if (score >= 30) label = "Weak";

  return { score: Math.min(100, score), label, checks };
}
