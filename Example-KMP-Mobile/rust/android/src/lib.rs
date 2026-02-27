use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use jni::errors::ThrowRuntimeExAndDefault;
use jni::EnvUnowned;
use rustcore::{fibonacci, rust_greeting};

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_examplemobilekmp_rustrodroid_RustBridge_rustGreeting<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    name: JString<'caller>,
) -> jstring {
    unowned_env.with_env(|env| -> jni::errors::Result<_> {
        let name: String = env.get_string(&name)?.into();
        let result = rust_greeting(&name);
        Ok(env.new_string(result)?.into_raw())
    }).resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_examplemobilekmp_rustrodroid_RustBridge_fibonacci<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    n: jint,
) -> jlong {
    unowned_env.with_env(|_env| -> jni::errors::Result<_> {
        Ok(fibonacci(n as u32) as jlong)
    }).resolve::<ThrowRuntimeExAndDefault>()
}
