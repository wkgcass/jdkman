use std::process::{Command, Stdio};

fn main() {
  let mut args: Vec<_> = std::env::args().collect();
  if args.len() == 0 {
    println!("{}", "failed to determine the program to be executed: empty arguments list");
    std::process::exit(1)
  }
  let mut name = args[0].clone();
  if name.contains("/") {
    let idx = name.rfind("/").unwrap();
    name = name.split_off(idx + 1);
  }
  if name.contains("\\") {
    let idx = name.rfind("\\").unwrap();
    name = name.split_off(idx + 1);
  }

  let output = Command::new("jdkman").arg("which")
    .output()
    .expect("Failed to execute `jdkman which`");
  let stdout = String::from_utf8(output.stdout)
    .expect("Invalid UTF-8 output from `jdkman which`");
  if !output.status.success() {
    println!("failed to run `jdkman which`, stdout: {}", stdout);
    std::process::exit(output.status.code().unwrap_or(1));
  }
  let javahome = stdout.trim();

  let mut exe_path_buf = std::path::PathBuf::new();
  exe_path_buf.push(javahome);
  exe_path_buf.push("bin");
  exe_path_buf.push(name);
  let exe = exe_path_buf.into_os_string().into_string().unwrap();
  args.remove(0);

  let mut child = Command::new(exe.clone())
    .args(args)
    .stdin(Stdio::inherit())
    .stdout(Stdio::inherit())
    .stderr(Stdio::inherit())
    .spawn().expect(format!("Failed to execute command {}", exe).as_str());

  let exit_status = child.wait().unwrap();
  std::process::exit(exit_status.code().unwrap());
}
