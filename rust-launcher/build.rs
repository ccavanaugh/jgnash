// adds an icon to the windows executable

extern crate winres;

fn main() {
    if cfg!(target_os = "windows") {
        let mut res = winres::WindowsResource::new();
        res.set_icon("gnome-money.ico");
        res.compile().unwrap();
    }
}
