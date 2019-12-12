#![windows_subsystem = "windows"]

extern crate java_locator;
extern crate msgbox;

use std::env;
use std::f32;
use std::ops::Add;
use std::path::PathBuf;
use std::process;
use std::process::Command;

use msgbox::IconType;

const MIN_JAVA_VERSION: f32 = 11.0 - (2.0 * f32::EPSILON);

fn main() {
    let v = get_java_version();

    if v >= MIN_JAVA_VERSION {
        let java_home = java_locator::locate_java_home();

        match java_home {
            Ok(_s) => launch_jgnash(),
            Err(_e) => msgbox::create(
                "Error",
                "Unable to locate a valid Java installation.\n\n
                 Please check your download a JVM from https://adoptopenjdk.net.",
                IconType::Error,
            ),
        }
    } else {
        msgbox::create(
            "Error",
            "Your Java installation is too old or misconfigured.\n\n\
             Please download a JVM from https://adoptopenjdk.net.",
            IconType::Error,
        )
    }
}

#[cfg(target_family = "windows")]
fn launch_jgnash() {
    // collect environment variables; the fist is the path that launched the program
    let mut args: Vec<String> = env::args().collect();
    args.remove(0);

    //let class_path = "c:\\temp\\jGnash-3.4.0\\lib\\*";

    let class_path = get_execution_path()
        .as_os_str()
        .to_str()
        .unwrap()
        .to_string()
        .add("\\lib\\*");

    let status = Command::new(get_java_exe())
        .arg("-classpath")
        .arg(&class_path)
        .arg("jGnash")
        .arg(args.join(" "))
        .status()
        .expect("command failed to start");

    process::exit(status.code().unwrap());
}

#[cfg(target_family = "unix")]
fn launch_jgnash() {
    // collect environment variables; the fist is the path that launched the program
    let mut args: Vec<String> = env::args().collect();
    args.remove(0);

    let class_path = get_execution_path()
        .as_os_str()
        .to_str()
        .unwrap()
        .to_string()
        .add("/lib/*");

    let status = Command::new(get_java_exe())
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

/// Executes java --version and extracts the version
fn get_java_version() -> f32 {
    let mut version = -1.0_f32;

    let output = Command::new(get_java_exe())
        .arg("--version")
        .output()
        .expect("failed to execute command");

    let st = String::from_utf8(output.stdout).unwrap();

    if !st.is_empty() {
        // look for the first available number
        let mut decimal_found = false;
        let mut version_string = String::new();

        // munch through the string one char at a time and extract the first decimal
        for c in st.chars() {
            if c.is_ascii_digit() {
                version_string.push(c);
            }

            if c == '.' {
                if decimal_found {
                    // we don't want the tertiary decimal
                    break;
                }

                version_string.push(c);
                decimal_found = true;
            }

            // must have found a xx.xx decimal instead of xx.xx.x
            if c.is_ascii_whitespace() && decimal_found {
                break;
            }
        }

        if !version_string.is_empty() {
            version = match version_string.parse::<f32>() {
                Ok(f) => f,
                Err(_e) => {
                    eprintln!("failed to parse: {}", version_string);
                    version
                }
            }
        }
    }

    return version;
}

#[cfg(target_family = "unix")]
fn get_java_exe() -> String {
    java_locator::locate_java_home().unwrap().add("/bin/java")
}

#[cfg(target_family = "windows")]
fn get_java_exe() -> String {
    java_locator::locate_java_home().unwrap().add("\\bin\\javaw.exe")
}
