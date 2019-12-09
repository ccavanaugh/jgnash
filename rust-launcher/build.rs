// adds an icon to the windows executable

#[cfg(target_family = "windows")]
extern crate winres;

#[cfg(target_family = "windows")]
fn main() {
    if cfg!(target_os = "windows") {
        let mut res = winres::WindowsResource::new();
        res.set_icon("gnome-money.ico");
        res.compile().unwrap();
    }
}

#[cfg(target_family = "unix")]
fn main() {
    // empty for unix build
}
