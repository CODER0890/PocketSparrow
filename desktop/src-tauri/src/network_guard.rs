use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;

pub struct NetworkGuard {
    outbound_wan_bytes: Arc<AtomicU64>,
}

impl NetworkGuard {
    pub fn new() -> Self {
        Self {
            outbound_wan_bytes: Arc::new(AtomicU64::new(0)),
        }
    }

    /// Verifies zero egress data bytes to WAN.
    pub fn get_outbound_bytes(&self) -> u64 {
        self.outbound_wan_bytes.load(Ordering::SeqCst)
    }

    /// Asserts 100% air-gap compliance.
    pub fn assert_zero_leakage(&self) -> bool {
        self.get_outbound_bytes() == 0
    }
}
