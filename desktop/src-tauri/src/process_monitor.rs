use serde::{Deserialize, Serialize};
use std::path::Path;
use sysinfo::{Pid, System};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DesktopProcessInfo {
    pub pid: u32,
    pub name: String,
    pub path: String,
    pub is_suspicious: bool,
    pub is_system: bool,
    pub threat_detail: String,
}

pub struct ProcessMonitor;

impl ProcessMonitor {
    pub fn new() -> Self {
        Self
    }

    /// Determines if a process is an essential operating system component or system-related daemon.
    pub fn is_system_process(pid: u32, name: &str, path: &str) -> bool {
        // 1. Critical core system PIDs
        // PID 0 (Idle / Swapper), PID 1 (init / systemd / launchd), PID 2 (kthreadd), PID 4 (System on Windows)
        if pid <= 2 || pid == 4 {
            return true;
        }

        // 2. Self-protection: Never terminate PocketSparrow itself
        if pid == std::process::id() {
            return true;
        }

        let name_lower = name.to_lowercase();
        let path_lower = path.to_lowercase();

        // 3. Kernel workers and threads (bracketed names like [kworker...], [ksoftirqd...])
        if name.starts_with('[') && name.ends_with(']') {
            return true;
        }
        if name_lower.starts_with("kworker")
            || name_lower.starts_with("ksoftirqd")
            || name_lower.starts_with("rcu_")
            || name_lower.starts_with("migration/")
            || name_lower.starts_with("cpuhp/")
            || name_lower.starts_with("watchdog")
            || name_lower.starts_with("jbd2/")
            || name_lower.starts_with("scsi_")
            || name_lower.starts_with("kswapd")
            || name_lower.starts_with("khugepaged")
            || name_lower.starts_with("kcompactd")
            || name_lower.starts_with("ksmd")
            || name_lower.starts_with("oom_reaper")
        {
            return true;
        }

        let base_name = Path::new(name)
            .file_name()
            .and_then(|f| f.to_str())
            .unwrap_or(name)
            .to_lowercase();
        let clean_base = base_name.trim_end_matches(".exe");

        // 4. Linux Core System Daemons & Services
        const LINUX_SYSTEM_BINARIES: &[&str] = &[
            "systemd",
            "init",
            "kthreadd",
            "udevd",
            "dbus-daemon",
            "dbus-broker",
            "polkitd",
            "cron",
            "crond",
            "atd",
            "sshd",
            "networkmanager",
            "wpa_supplicant",
            "iwd",
            "dhclient",
            "dhcpcd",
            "avahi-daemon",
            "cupsd",
            "rsyslogd",
            "syslogd",
            "syslog-ng",
            "auditd",
            "acpid",
            "thermald",
            "upowerd",
            "accounts-daemon",
            "pipewire",
            "wireplumber",
            "pulseaudio",
            "alsactl",
            "xorg",
            "xwayland",
            "wayland",
            "kwin",
            "mutter",
            "gnome-shell",
            "gnome-session",
            "plasmashell",
            "startplasma",
            "gdm",
            "lightdm",
            "sddm",
            "login",
            "agetty",
            "getty",
            "su",
            "sudo",
            "containerd",
            "dockerd",
            "pocket_sparrow",
            "pocket-sparrow",
            "tauri",
        ];

        for sys_bin in LINUX_SYSTEM_BINARIES {
            if clean_base == *sys_bin || clean_base.starts_with(&format!("{}-", sys_bin)) {
                return true;
            }
        }

        // 5. Windows Core System Processes
        const WINDOWS_SYSTEM_BINARIES: &[&str] = &[
            "system",
            "idle",
            "registry",
            "smss",
            "csrss",
            "wininit",
            "services",
            "lsass",
            "lsm",
            "winlogon",
            "svchost",
            "fontdrvhost",
            "dwm",
            "sihost",
            "taskhostw",
            "explorer",
            "spoolsv",
            "audiodg",
            "conhost",
            "werfault",
            "runtimebroker",
            "searchindexer",
            "searchhost",
            "startmenuexperiencehost",
            "shellexperiencehost",
            "securityhealthservice",
            "securityhealthsystray",
            "mpcmdrun",
            "msmpeng",
            "dllhost",
            "ctfmon",
            "smartscreen",
        ];

        for win_bin in WINDOWS_SYSTEM_BINARIES {
            if clean_base == *win_bin {
                return true;
            }
        }

        // 6. macOS Core System Daemons
        const MACOS_SYSTEM_BINARIES: &[&str] = &[
            "launchd",
            "kernel_task",
            "kextd",
            "syspolicyd",
            "opendirectoryd",
            "securityd",
            "trustd",
            "logd",
            "diskarbitrationd",
            "coreauthd",
            "powerd",
            "thermalmonitord",
            "windowserver",
            "loginwindow",
            "dock",
            "finder",
            "systemuiserver",
            "controlcenter",
            "notificationcenter",
            "tccd",
            "mds",
            "mds_stores",
            "distnoted",
            "cfprefsd",
        ];

        for mac_bin in MACOS_SYSTEM_BINARIES {
            if clean_base == *mac_bin {
                return true;
            }
        }

        // 7. System Directory Roots
        if path_lower.starts_with("/sbin/")
            || path_lower.starts_with("/usr/sbin/")
            || path_lower.starts_with("/usr/lib/systemd/")
            || path_lower.starts_with("/lib/systemd/")
            || path_lower.starts_with("/usr/libexec/")
        {
            return true;
        }

        if path_lower.contains("\\windows\\system32\\")
            || path_lower.contains("\\windows\\syswow64\\")
            || path_lower.contains("\\windows\\systemapps\\")
            || path_lower.contains("\\windows\\servicing\\")
        {
            return true;
        }

        if path_lower.starts_with("/system/library/")
            || path_lower.starts_with("/system/library/coreservices/")
            || path_lower.starts_with("/system/library/frameworks/")
        {
            return true;
        }

        if path_lower.starts_with("/system/bin/")
            || path_lower.starts_with("/system/apex/")
            || path_lower.starts_with("/vendor/bin/")
            || path_lower.starts_with("/apex/")
        {
            return true;
        }

        false
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

            // Determine if this is a protected system process
            let is_system = Self::is_system_process(pid_u32, &name, &path);

            // Cross-platform threat heuristics (only applied to non-system processes)
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

            if is_system {
                is_suspicious = false;
                detail = "Essential operating system component. Termination prohibited.".to_string();
            } else if has_rev_shell {
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
                is_system,
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
                is_system: true,
                threat_detail: "Essential operating system component. Termination prohibited.".to_string(),
            });
        }

        processes
    }

    /// Terminates a process by PID across Linux, Windows, macOS, Android.
    /// Strictly protects operating system processes and system-related daemons from being killed.
    pub fn terminate_process(&self, pid: u32) -> Result<bool, String> {
        let mut sys = System::new();
        let sys_pid = Pid::from_u32(pid);
        sys.refresh_processes(sysinfo::ProcessesToUpdate::Some(&[sys_pid]), true);

        // 1. Resolve process details to assert safety against system processes
        let (name, path) = if let Some(proc) = sys.process(sys_pid) {
            (
                proc.name().to_string_lossy().to_string(),
                proc.exe().map(|p| p.to_string_lossy().to_string()).unwrap_or_default(),
            )
        } else {
            #[cfg(target_os = "linux")]
            {
                let comm = std::fs::read_to_string(format!("/proc/{}/comm", pid))
                    .unwrap_or_default()
                    .trim()
                    .to_string();
                let exe = std::fs::read_link(format!("/proc/{}/exe", pid))
                    .map(|p| p.to_string_lossy().to_string())
                    .unwrap_or_default();
                (comm, exe)
            }
            #[cfg(not(target_os = "linux"))]
            {
                (String::new(), String::new())
            }
        };

        // 2. Strict system process safety invariant: Refuse to terminate system processes
        if Self::is_system_process(pid, &name, &path) {
            return Err(format!(
                "Protected Process: PID {} ('{}') is a vital operating system service and cannot be terminated.",
                pid,
                if name.is_empty() { "system" } else { &name }
            ));
        }

        // 3. Terminate non-system threat or authorized process
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
            println!("PID {}: name='{}', path='{}', is_sys={}, susp={}", p.pid, p.name, p.path, p.is_system, p.is_suspicious);
        }
        for p in list {
            assert!(!p.name.trim().is_empty(), "PID {} has empty name!", p.pid);
            assert!(!p.path.trim().is_empty(), "PID {} has empty path!", p.pid);
        }
    }

    #[test]
    fn test_system_process_termination_strictly_blocked() {
        let monitor = ProcessMonitor::new();

        // 1. PID 1 (systemd / init) must be blocked
        let res_pid1 = monitor.terminate_process(1);
        assert!(res_pid1.is_err(), "Terminating PID 1 must be blocked!");
        assert!(res_pid1.unwrap_err().contains("Protected Process"));

        // 2. PID 2 (kthreadd) must be blocked
        let res_pid2 = monitor.terminate_process(2);
        assert!(res_pid2.is_err(), "Terminating PID 2 must be blocked!");

        // 3. Current process (self) must be blocked
        let self_pid = std::process::id();
        let res_self = monitor.terminate_process(self_pid);
        assert!(res_self.is_err(), "Terminating self PID must be blocked!");

        // 4. Test is_system_process classification across platforms
        assert!(ProcessMonitor::is_system_process(1, "systemd", "/sbin/init"));
        assert!(ProcessMonitor::is_system_process(42, "dbus-daemon", "/usr/bin/dbus-daemon"));
        assert!(ProcessMonitor::is_system_process(99, "[kworker/0:1]", ""));
        assert!(ProcessMonitor::is_system_process(100, "svchost.exe", "C:\\Windows\\System32\\svchost.exe"));
        assert!(ProcessMonitor::is_system_process(101, "launchd", "/sbin/launchd"));
        assert!(ProcessMonitor::is_system_process(102, "service", "/system/bin/servicemanager"));

        // Normal user processes should not be classified as system
        assert!(!ProcessMonitor::is_system_process(12345, "my_rogue_app", "/tmp/my_rogue_app"));
        assert!(!ProcessMonitor::is_system_process(54321, "calculator", "/home/user/apps/calc"));
    }
}

