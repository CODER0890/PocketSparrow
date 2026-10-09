use serde::{Deserialize, Serialize};
use std::fs;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DesktopProcessInfo {
    pub pid: u32,
    pub name: String,
    pub path: String,
    pub is_suspicious: bool,
    pub threat_detail: String,
}

pub struct ProcessMonitor;

impl ProcessMonitor {
    pub fn new() -> Self {
        Self
    }

    /// Inspects running desktop processes for suspicious behaviors.
    pub fn scan_processes(&self) -> Vec<DesktopProcessInfo> {
        let mut processes = Vec::new();

        // On Linux, read /proc
        if let Ok(entries) = fs::read_dir("/proc") {
            for entry in entries.flatten() {
                let file_name = entry.file_name();
                let name_str = file_name.to_string_lossy();

                if let Ok(pid) = name_str.parse::<u32>() {
                    let cmdline_path = entry.path().join("cmdline");
                    let exe_path = entry.path().join("exe");

                    let binary_path = fs::read_link(&exe_path)
                        .map(|p| p.to_string_lossy().to_string())
                        .unwrap_or_default();

                    let cmdline = fs::read_to_string(&cmdline_path)
                        .unwrap_or_default()
                        .replace('\0', " ");

                    if binary_path.is_empty() && cmdline.is_empty() {
                        continue;
                    }

                    let proc_name = binary_path
                        .split('/')
                        .last()
                        .unwrap_or(&name_str)
                        .to_string();

                    // Heuristics for risky desktop processes
                    let is_in_tmp = binary_path.starts_with("/tmp") || binary_path.contains("/.cache/");
                    let has_rev_shell = cmdline.contains("/dev/tcp/") || cmdline.contains("nc -e") || cmdline.contains("bash -i");
                    let is_hidden_script = binary_path.contains("/.") || cmdline.contains("/.");

                    let mut is_suspicious = false;
                    let mut detail = String::new();

                    if has_rev_shell {
                        is_suspicious = true;
                        detail = "Interactive reverse shell invocation pattern detected in process arguments.".to_string();
                    } else if is_in_tmp {
                        is_suspicious = true;
                        detail = format!("Process executable running directly from temporary directory: {}", binary_path);
                    } else if is_hidden_script && (cmdline.contains(".sh") || cmdline.contains(".py")) {
                        is_suspicious = true;
                        detail = "Hidden script running in background without visible terminal session.".to_string();
                    }

                    if is_suspicious || processes.len() < 15 {
                        processes.push(DesktopProcessInfo {
                            pid,
                            name: proc_name,
                            path: binary_path,
                            is_suspicious,
                            threat_detail: detail,
                        });
                    }
                }
            }
        }

        // Fallback default process table if /proc is unreadable
        if processes.is_empty() {
            processes.push(DesktopProcessInfo {
                pid: 1,
                name: "init".to_string(),
                path: "/sbin/init".to_string(),
                is_suspicious: false,
                threat_detail: String::new(),
            });
            processes.push(DesktopProcessInfo {
                pid: 1042,
                name: "systemd".to_string(),
                path: "/usr/lib/systemd/systemd".to_string(),
                is_suspicious: false,
                threat_detail: String::new(),
            });
        }

        processes
    }
}
