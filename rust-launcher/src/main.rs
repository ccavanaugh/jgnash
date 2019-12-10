#![windows_subsystem = "windows"]

extern crate java_locator;
extern crate msgbox;

use std::env;
use std::ops::Add;
use std::path::PathBuf;
use std::process;
use std::process::Command;

use msgbox::IconType;

fn main() {
    let java_home = java_locator::locate_java_home();

    match java_home {
        Ok(s) => launch_jgnash(s),
        Err(_e) => msgbox::create(
            "Error",
            "Unable to locate a valid Java installation.\n\n\
             Please download a JVM from https://adoptopenjdk.net.",
            IconType::Error,
        ),
    }
}

#[cfg(target_family = "windows")]
fn launch_jgnash(s: String) {

    // collect environment variables; the fist is the path that launched the program
    let mut args: Vec<String> = env::args().collect();
    args.remove(0);

    // the java executable
    let java_exe = s.add("\\bin\\javaw.exe");

    //let class_path = "c:\\temp\\jGnash-3.4.0\\lib\\*";

    let class_path = get_execution_path()
        .as_os_str()
        .to_str()
        .unwrap()
        .to_string()
        .add("\\lib\\*");

    let status = Command::new(&java_exe)
        .arg("-classpath")
        .arg(&class_path)
        .arg("jGnash")
        .arg(args.join(" "))
        .status()
        .expect("command failed to start");

    process::exit(status.code().unwrap());
}

#[cfg(target_family = "unix")]
fn launch_jgnash(s: String) {

    // collect environment variables; the fist is the path that launched the program
    let mut args: Vec<String> = env::args().collect();
    args.remove(0);

    // the java executable
    let java_exe = s.add("/bin/java");

    let class_path = get_execution_path()
        .as_os_str()
        .to_str()
        .unwrap()
        .to_string()
        .add("/lib/*");

    let status = Command::new(&java_exe)
        .arg("-classpath")
        .arg(&class_path)
        .arg("jGnash")
        .arg(args.join(" "))
        .status()
        .expect("command failed to start");

    process::exit(status.code().unwrap());
}

fn get_execution_path() -> PathBuf {
    match env::current_exe() {
        Ok(mut path) => {
            path.pop(); // pop off the name of the executable
            path
        }
        Err(_e) => PathBuf::new(),
    }
}
