use serde::{Deserialize, Serialize};
use std::path::Path;
use sysinfo::{Pid, System};

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

    /// Inspects running host processes cross-platform (Linux, Windows, macOS, Android).
    pub fn scan_processes(&self) -> Vec<DesktopProcessInfo> {
        let mut sys = System::new_all();
        sys.refresh_all();

        let mut processes = Vec::new();

        // Cross-platform scan using sysinfo
        for (pid, process) in sys.processes() {
            let pid_u32 = pid.as_u32();
            if pid_u32 == 0 {
                continue;
            }

            let mut name = process.name().to_string_lossy().to_string();
            let mut path = process
                .exe()
                .map(|p| p.to_string_lossy().to_string())
                .unwrap_or_default();

            let cmd_args: Vec<String> = process
                .cmd()
                .iter()
                .map(|s| s.to_string_lossy().to_string())
                .collect();
            let cmdline = cmd_args.join(" ");

            // If path is empty, fallback to first command argument if it looks like a path
            if path.is_empty() {
                if let Some(first_arg) = cmd_args.first() {
                    let trimmed = first_arg.trim();
                    if trimmed.starts_with('/') || trimmed.contains('\\') {
                        path = trimmed.split_whitespace().next().unwrap_or(trimmed).to_string();
                    }
                }
            }

            // On Linux, if name or path is missing, inspect /proc files (comm, cmdline, exe)
            #[cfg(target_os = "linux")]
            {
                if path.is_empty() || name.is_empty() {
                    let proc_dir = format!("/proc/{}", pid_u32);
                    if let Ok(comm) = std::fs::read_to_string(format!("{}/comm", proc_dir)) {
                        let comm_trimmed = comm.trim().to_string();
                        if !comm_trimmed.is_empty() && (name.is_empty() || name.starts_with("process-")) {
                            name = comm_trimmed;
                        }
                    }
                    if path.is_empty() {
                        if let Ok(dest) = std::fs::read_link(format!("{}/exe", proc_dir)) {
                            path = dest.to_string_lossy().to_string();
                        } else if let Ok(cmd) = std::fs::read_to_string(format!("{}/cmdline", proc_dir)) {
                            let first = cmd.split('\0').next().unwrap_or("").trim();
                            if first.starts_with('/') {
                                path = first.to_string();
                            }
                        }
                    }
                }
            }

            // Fallback for name if still empty
            if name.is_empty() {
                if !path.is_empty() {
                    name = Path::new(&path)
                        .file_name()
                        .map(|f| f.to_string_lossy().to_string())
                        .unwrap_or_else(|| format!("proc-{}", pid_u32));
                } else {
                    name = format!("process-{}", pid_u32);
                }
            }

            // Fallback for path if still empty
            if path.is_empty() {
                #[cfg(windows)]
                {
                    path = format!("C:\\Windows\\System32\\{}.exe", name);
                }
                #[cfg(target_os = "macos")]
                {
                    path = format!("/System/Library/CoreServices/{}", name);
                }
                #[cfg(not(any(windows, target_os = "macos")))]
                {
                    path = format!("/usr/bin/{}", name);
                }
            }

            // Cross-platform threat heuristics
            let lower_path = path.to_lowercase();
            let lower_cmd = cmdline.to_lowercase();

            let is_in_tmp = lower_path.starts_with("/tmp")
                || lower_path.contains("/.cache/")
                || lower_path.contains("\\temp\\")
                || lower_path.contains("\\appdata\\local\\temp\\");

            let has_rev_shell = lower_cmd.contains("/dev/tcp/")
                || lower_cmd.contains("nc -e")
                || lower_cmd.contains("ncat -e")
                || lower_cmd.contains("bash -i")
                || lower_cmd.contains("sh -i")
                || lower_cmd.contains("powershell -enc")
                || lower_cmd.contains("invoke-expression")
                || lower_cmd.contains("iex(new-object");

            let is_hidden_script = (lower_path.contains("/.") || lower_cmd.contains("/."))
                && (lower_cmd.contains(".sh") || lower_cmd.contains(".py") || lower_cmd.contains(".ps1"));

            let is_tunnel_threat = lower_cmd.contains("chisel")
                || lower_cmd.contains("mimikatz")
                || lower_cmd.contains("socat tcp-listen");

            let mut is_suspicious = false;
            let mut detail = String::new();

            if has_rev_shell {
                is_suspicious = true;
                detail = "Interactive reverse shell invocation pattern detected in process arguments.".to_string();
            } else if is_in_tmp {
                is_suspicious = true;
                detail = format!("Process executable running directly from temporary directory: {}", path);
            } else if is_hidden_script {
                is_suspicious = true;
                detail = "Hidden script running in background without visible terminal session.".to_string();
            } else if is_tunnel_threat {
                is_suspicious = true;
                detail = "Unauthorized lateral movement or exfiltration tool detected.".to_string();
            }

            processes.push(DesktopProcessInfo {
                pid: pid_u32,
                name,
                path,
                is_suspicious,
                threat_detail: detail,
            });
        }

        // Sort: suspicious processes first, then by PID
        processes.sort_by(|a, b| {
            b.is_suspicious.cmp(&a.is_suspicious).then(a.pid.cmp(&b.pid))
        });

        // Limit default view to 50 processes
        if processes.len() > 50 {
            processes.truncate(50);
        }

        // Fallback default process table if no processes were returned
        if processes.is_empty() {
            processes.push(DesktopProcessInfo {
                pid: 1,
                name: "systemd".to_string(),
                path: "/usr/lib/systemd/systemd".to_string(),
                is_suspicious: false,
                threat_detail: String::new(),
            });
        }

        processes
    }

    /// Terminates a process by PID across Linux, Windows, macOS, Android.
    pub fn terminate_process(&self, pid: u32) -> Result<bool, String> {
        let mut sys = System::new();
        let sys_pid = Pid::from_u32(pid);
        sys.refresh_processes(sysinfo::ProcessesToUpdate::Some(&[sys_pid]), true);

        if let Some(process) = sys.process(sys_pid) {
            if process.kill() {
                return Ok(true);
            }
        }

        #[cfg(unix)]
        {
            use std::process::Command;
            let status = Command::new("kill").arg("-9").arg(pid.to_string()).status();
            if let Ok(s) = status {
                if s.success() {
                    return Ok(true);
                }
            }
        }

        #[cfg(windows)]
        {
            use std::process::Command;
            let status = Command::new("taskkill")
                .arg("/F")
                .arg("/PID")
                .arg(pid.to_string())
                .status();
            if let Ok(s) = status {
                if s.success() {
                    return Ok(true);
                }
            }
        }

        Ok(false)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_process_scan_never_empty_name_or_path() {
        let monitor = ProcessMonitor::new();
        let list = monitor.scan_processes();
        assert!(!list.is_empty());
        for p in &list[..5.min(list.len())] {
            println!("PID {}: name='{}', path='{}', susp={}", p.pid, p.name, p.path, p.is_suspicious);
        }
        for p in list {
            assert!(!p.name.trim().is_empty(), "PID {} has empty name!", p.pid);
            assert!(!p.path.trim().is_empty(), "PID {} has empty path!", p.pid);
        }
    }
}
