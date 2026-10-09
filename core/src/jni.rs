use crate::engine::DetectionEngine;
use crate::types::{ContentType, ScanResult};
use jni::objects::{JClass, JObjectArray, JString};
use jni::sys::{jint, jlong, jobject};
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_com_pocketsparrow_core_NativeBridge_initEngine(
    mut env: JNIEnv,
    _class: JClass,
    model_path: JString,
    vocab_path: JString,
) -> jlong {
    let m_path: Option<String> = if !model_path.is_null() {
        env.get_string(&model_path).ok().map(|s| s.into())
    } else {
        None
    };

    let v_path: Option<String> = if !vocab_path.is_null() {
        env.get_string(&vocab_path).ok().map(|s| s.into())
    } else {
        None
    };

    let engine = Box::new(DetectionEngine::new(m_path.as_deref(), v_path.as_deref()));
    Box::into_raw(engine) as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_pocketsparrow_core_NativeBridge_freeEngine(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        unsafe {
            let _ = Box::from_raw(handle as *mut DetectionEngine);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_pocketsparrow_core_NativeBridge_scanContent(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    content_type: jint,
    payload: JString,
) -> jobject {
    if handle == 0 || payload.is_null() {
        return std::ptr::null_mut();
    }

    let engine = unsafe { &*(handle as *const DetectionEngine) };
    let ctype = ContentType::from_i32(content_type);
    let payload_str: String = match env.get_string(&payload) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    let result = engine.scan(ctype, &payload_str);
    convert_to_java_result(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_pocketsparrow_core_NativeBridge_auditPermissions(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    permissions: JObjectArray,
) -> jobject {
    if handle == 0 || permissions.is_null() {
        return std::ptr::null_mut();
    }

    let engine = unsafe { &*(handle as *const DetectionEngine) };
    let len = match env.get_array_length(&permissions) {
        Ok(l) => l,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut perm_strings = Vec::with_capacity(len as usize);
    for i in 0..len {
        if let Ok(item) = env.get_object_array_element(&permissions, i) {
            let jstr: JString = item.into();
            let parsed_opt = {
                let java_str_res = env.get_string(&jstr);
                java_str_res.ok().map(|s| s.to_str().unwrap_or("").to_string())
            };
            if let Some(s) = parsed_opt {
                perm_strings.push(s);
            }
        }
    }

    let perm_slices: Vec<&str> = perm_strings.iter().map(|s: &String| s.as_str()).collect();
    let result = engine.audit_permissions(&perm_slices);
    convert_to_java_result(&mut env, result)
}

fn convert_to_java_result(env: &mut JNIEnv, res: ScanResult) -> jobject {
    let class = match env.find_class("com/pocketsparrow/core/ScanResult") {
        Ok(c) => c,
        Err(_) => return std::ptr::null_mut(),
    };

    let j_category = match env.new_string(&res.category) {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    let j_reason = match env.new_string(&res.xai_reason) {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    let threat_int: jint = res.threat_level as i32;
    let tier_int: jint = res.tier_triggered as i32;
    let conf: f32 = res.confidence;
    let latency: jlong = res.latency_us as i64;
    let should_block: bool = res.should_block;

    let obj = env.new_object(
        class,
        "(IIFJLjava/lang/String;Ljava/lang/String;Z)V",
        &[
            threat_int.into(),
            tier_int.into(),
            conf.into(),
            latency.into(),
            (&j_category).into(),
            (&j_reason).into(),
            should_block.into(),
        ],
    );

    match obj {
        Ok(o) => o.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}
