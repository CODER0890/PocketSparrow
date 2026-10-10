pub mod call_screener;
pub mod email_screener;
pub mod sms_screener;

pub use call_screener::CallScreener;
pub use email_screener::EmailScreener;
pub use sms_screener::SmsScreener;

use crate::engine::DetectionEngine;
use crate::types::{CallSignals, CallVerdict, EmailSignals, EmailVerdict, SmsSignals, SmsVerdict};
use std::sync::Arc;

pub struct CommunicationShield {
    call_screener: CallScreener,
    sms_screener: SmsScreener,
    email_screener: EmailScreener,
}

impl CommunicationShield {
    pub fn new(engine: Arc<DetectionEngine>) -> Self {
        Self {
            call_screener: CallScreener::new(),
            sms_screener: SmsScreener::new(engine.clone()),
            email_screener: EmailScreener::new(engine),
        }
    }

    /// Evaluates incoming phone call against on-device reputation and STIR/SHAKEN
    pub fn evaluate_call(&self, signals: &CallSignals) -> CallVerdict {
        self.call_screener.screen_call(signals)
    }

    /// Evaluates incoming SMS/RCS message with on-device heuristics & MobileBERT
    pub fn evaluate_sms(&self, signals: &SmsSignals) -> SmsVerdict {
        self.sms_screener.screen_sms(signals)
    }

    /// Evaluates incoming email with SPF/DKIM/DMARC headers, spoofing checks, and link scans
    pub fn evaluate_email(&self, signals: &EmailSignals) -> EmailVerdict {
        self.email_screener.screen_email(signals)
    }
}
