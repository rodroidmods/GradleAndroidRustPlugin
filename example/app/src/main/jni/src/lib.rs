use jni::sys::jstring;
use jni::objects::JObject;
use jni::errors::ThrowRuntimeExAndDefault;
use jni::EnvUnowned;

#[unsafe(no_mangle)]
pub extern "C" fn Java_dev_rodroid_rust_MainActivity_callRustCode<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    _: JObject<'caller>,
) -> jstring {
    unowned_env.with_env(|env| -> jni::errors::Result<_> {
        let message = "Hello from Rust";
        let java_string = env.new_string(message)?;
        Ok(java_string.into_raw())
    }).resolve::<ThrowRuntimeExAndDefault>()
}
