use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;
use rustcore::{fibonacci, rust_greeting};

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_examplemobilekmp_rustrodroid_RustBridge_rustGreeting(
    mut env: JNIEnv,
    _class: JClass,
    name: JString,
) -> jstring {
    let name: String = env.get_string(&name).expect("Invalid string").into();
    let result = rust_greeting(&name);
    env.new_string(result).expect("Failed to create string").into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_examplemobilekmp_rustrodroid_RustBridge_fibonacci(
    _env: JNIEnv,
    _class: JClass,
    n: jint,
) -> jlong {
    fibonacci(n as u32) as jlong
}
