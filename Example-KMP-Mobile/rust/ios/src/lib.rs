use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use rustcore::{fibonacci, rust_greeting};

#[unsafe(no_mangle)]
pub extern "C" fn rust_greeting_c(name: *const c_char) -> *mut c_char {
    let name = unsafe {
        if name.is_null() {
            "Unknown"
        } else {
            CStr::from_ptr(name).to_str().unwrap_or("Unknown")
        }
    };
    let result = rust_greeting(name);
    CString::new(result).unwrap().into_raw()
}

#[unsafe(no_mangle)]
pub extern "C" fn rust_greeting_free(s: *mut c_char) {
    if !s.is_null() {
        unsafe {
            let _ = CString::from_raw(s);
        }
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn fibonacci_c(n: u32) -> u64 {
    fibonacci(n)
}
