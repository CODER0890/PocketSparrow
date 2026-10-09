use pocket_sparrow::engine::DetectionEngine;
use pocket_sparrow::types::ContentType;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpListener;

#[derive(Deserialize)]
struct BridgeScanRequest {
    url: String,
}

#[derive(Serialize)]
struct BridgeScanResponse {
    verdict: String,
    should_block: bool,
    category: String,
    xai_reason: String,
    latency_us: u32,
}

pub struct BrowserBridge {
    engine: Arc<DetectionEngine>,
}

impl BrowserBridge {
    pub fn new(engine: Arc<DetectionEngine>) -> Self {
        Self { engine }
    }

    /// Spawns the local socket loopback bridge strictly bound to 127.0.0.1:41789.
    /// Operates with zero WAN network access.
    pub fn start(&self) {
        let engine = self.engine.clone();

        tokio::spawn(async move {
            let addr = "127.0.0.1:41789";
            let listener = match TcpListener::bind(addr).await {
                Ok(l) => {
                    println!("[POCKET SPARROW] Browser bridge listening on loopback http://{}", addr);
                    l
                }
                Err(e) => {
                    eprintln!("[POCKET SPARROW] Failed to bind browser bridge socket: {}", e);
                    return;
                }
            };

            loop {
                if let Ok((mut stream, _)) = listener.accept().await {
                    let eng = engine.clone();
                    tokio::spawn(async move {
                        let mut buffer = [0u8; 4096];
                        if let Ok(bytes_read) = stream.read(&mut buffer).await {
                            if bytes_read == 0 {
                                return;
                            }

                            let request_str = String::from_utf8_lossy(&buffer[..bytes_read]);

                            // Handle HTTP OPTIONS preflight
                            if request_str.starts_with("OPTIONS") {
                                let preflight = "HTTP/1.1 204 No Content\r\n\
Access-Control-Allow-Origin: *\r\n\
Access-Control-Allow-Methods: POST, OPTIONS\r\n\
Access-Control-Allow-Headers: Content-Type\r\n\r\n";
                                let _ = stream.write_all(preflight.as_bytes()).await;
                                return;
                            }

                            // Handle POST /scan
                            if request_str.starts_with("POST /scan") {
                                // Extract JSON body
                                if let Some(body_start) = request_str.find("\r\n\r\n") {
                                    let body = &request_str[body_start + 4..];
                                    if let Ok(req) = serde_json::from_str::<BridgeScanRequest>(body.trim()) {
                                        let result = eng.scan(ContentType::Url, &req.url);
                                        let response_obj = BridgeScanResponse {
                                            verdict: result.threat_level.to_str().to_string(),
                                            should_block: result.should_block,
                                            category: result.category,
                                            xai_reason: result.xai_reason,
                                            latency_us: result.latency_us,
                                        };

                                        if let Ok(json_res) = serde_json::to_string(&response_obj) {
                                            let http_res = format!(
                                                "HTTP/1.1 200 OK\r\n\
Content-Type: application/json\r\n\
Access-Control-Allow-Origin: *\r\n\
Content-Length: {}\r\n\r\n{}",
                                                json_res.len(),
                                                json_res
                                            );
                                            let _ = stream.write_all(http_res.as_bytes()).await;
                                            return;
                                        }
                                    }
                                }
                            }

                            // Default 400 Bad Request
                            let bad_req = "HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n";
                            let _ = stream.write_all(bad_req.as_bytes()).await;
                        }
                    });
                }
            }
        });
    }
}
